/*
 * Copyright 2024-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.grpc.server.scope;

import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.util.Assert;

import io.grpc.Context;

/**
 * A {@link Scope} lasting for the duration of a gRPC call. The beans of a call are held
 * in the call's {@link Context gRPC context}, so they are shared by every thread to which
 * that context is propagated, and are destroyed when the context is cancelled, that is
 * when the call completes or is cancelled. Beans are created on first use within a call;
 * nothing can be looked up outside of a call or once the call has ended.
 *
 * @author Fernando Rodrigues Ripol
 * @author Sharang Gupta
 * @see GrpcRequestScopeServerInterceptor
 * @see org.springframework.grpc.server.annotation.GrpcRequestScope
 */
public class GrpcRequestScope implements Scope {

	/**
	 * The name of the scope.
	 */
	public static final String SCOPE_NAME = "grpcRequest";

	private static final Log logger = LogFactory.getLog(GrpcRequestScope.class);

	private static final Context.Key<ScopedBeans> SCOPED_BEANS = Context.key("grpcRequestScope");

	/**
	 * Create the context of a new call, holding the beans of that call. They are
	 * destroyed when the context is cancelled.
	 * @param parent the current context
	 * @return the context to run the call in
	 */
	static Context newCallContext(Context parent) {
		ScopedBeans scopedBeans = new ScopedBeans();
		Context context = parent.withValue(SCOPED_BEANS, scopedBeans);
		context.addListener((cancelled) -> scopedBeans.destroy(), Runnable::run);
		return context;
	}

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		return currentScopedBeans().get(name, objectFactory);
	}

	@Override
	public @Nullable Object remove(String name) {
		return currentScopedBeans().remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
		currentScopedBeans().registerDestructionCallback(name, callback);
	}

	@Override
	public @Nullable Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public @Nullable String getConversationId() {
		return null;
	}

	private static ScopedBeans currentScopedBeans() {
		ScopedBeans scopedBeans = SCOPED_BEANS.get();
		Assert.state(scopedBeans != null,
				() -> "No gRPC call is active in the current context: the " + SCOPE_NAME
						+ " scope is only available within a call intercepted by GrpcRequestScopeServerInterceptor"
						+ " and in threads to which the call's io.grpc.Context has been propagated");
		return scopedBeans;
	}

	/**
	 * The beans of one call. Creation is serialized per bean name, not per call, and
	 * nothing can be looked up once the call's beans have been destroyed.
	 */
	private static final class ScopedBeans {

		private final ConcurrentMap<String, ScopedBean> beans = new ConcurrentHashMap<>();

		private volatile boolean destroyed;

		Object get(String name, ObjectFactory<?> objectFactory) {
			assertNotDestroyed();
			ScopedBean bean = this.beans.computeIfAbsent(name, (key) -> new ScopedBean(key, objectFactory));
			Object instance = bean.get();
			if (this.destroyed) {
				// Lost the race against the end of the call: nothing may outlive it.
				ScopedBean late = this.beans.remove(name);
				if (late != null) {
					late.destroy();
				}
				assertNotDestroyed();
			}
			return instance;
		}

		@Nullable
		Object remove(String name) {
			ScopedBean bean = this.beans.remove(name);
			return (bean != null) ? bean.getIfCreated() : null;
		}

		void registerDestructionCallback(String name, Runnable callback) {
			ScopedBean bean = this.beans.get(name);
			if (bean != null) {
				bean.setDestructionCallback(callback);
			}
		}

		void destroy() {
			this.destroyed = true;
			Iterator<ScopedBean> iterator = this.beans.values().iterator();
			while (iterator.hasNext()) {
				ScopedBean bean = iterator.next();
				iterator.remove();
				bean.destroy();
			}
		}

		private void assertNotDestroyed() {
			Assert.state(!this.destroyed,
					() -> "The gRPC call has completed and its " + SCOPE_NAME + " scoped beans have been destroyed");
		}

	}

	/**
	 * One bean of a call, created on first access and destroyed at most once.
	 */
	private static final class ScopedBean {

		private final String name;

		private final ObjectFactory<?> objectFactory;

		private @Nullable Object instance;

		private @Nullable Runnable destructionCallback;

		ScopedBean(String name, ObjectFactory<?> objectFactory) {
			this.name = name;
			this.objectFactory = objectFactory;
		}

		synchronized Object get() {
			if (this.instance == null) {
				this.instance = this.objectFactory.getObject();
			}
			return this.instance;
		}

		synchronized @Nullable Object getIfCreated() {
			return this.instance;
		}

		synchronized void setDestructionCallback(Runnable callback) {
			this.destructionCallback = callback;
		}

		synchronized void destroy() {
			Runnable callback = this.destructionCallback;
			this.destructionCallback = null;
			this.instance = null;
			if (callback != null) {
				try {
					callback.run();
				}
				catch (Throwable ex) {
					logger.error("Destruction callback of gRPC request scoped bean '" + this.name + "' failed", ex);
				}
			}
		}

	}

}
