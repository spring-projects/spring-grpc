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

package org.springframework.grpc.client;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.stub.AbstractStub;

/**
 * A {@link BeanPostProcessor} that processes {@link GrpcClient @GrpcClient} annotations
 * on fields and setter methods to inject gRPC clients (stubs or channels).
 * <p>
 * This processor will:
 * <ul>
 * <li>Scan beans for fields/methods annotated with {@link GrpcClient @GrpcClient}</li>
 * <li>Create the appropriate gRPC client stub or channel</li>
 * <li>Apply any specified interceptors</li>
 * <li>Inject the created client into the field/method</li>
 * </ul>
 *
 * @author Oleksandr Shevchenko
 * @see GrpcClient
 * @see GrpcClientFactory
 */
public class GrpcClientBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

	private @Nullable ApplicationContext applicationContext;

	private final Map<Class<?>, List<InjectionTarget>> injectionTargetsCache = new ConcurrentHashMap<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		Class<?> beanClass = bean.getClass();
		List<InjectionTarget> targets = findInjectionTargets(beanClass);

		for (InjectionTarget target : targets) {
			try {
				Object client = createClient(target);
				target.inject(bean, client);
			}
			catch (Exception ex) {
				throw new BeanCreationException(beanName, "Failed to inject gRPC client for " + target.getMember(), ex);
			}
		}

		return bean;
	}

	private List<InjectionTarget> findInjectionTargets(Class<?> beanClass) {
		return this.injectionTargetsCache.computeIfAbsent(beanClass, this::doFindInjectionTargets);
	}

	private List<InjectionTarget> doFindInjectionTargets(Class<?> beanClass) {
		List<InjectionTarget> targets = new ArrayList<>();

		ReflectionUtils.doWithFields(beanClass, field -> {
			GrpcClient annotation = AnnotationUtils.findAnnotation(field, GrpcClient.class);
			if (annotation != null) {
				if (Modifier.isStatic(field.getModifiers())) {
					throw new IllegalStateException(
							"@GrpcClient annotation is not supported on static fields: " + field);
				}
				targets.add(new FieldInjectionTarget(field, annotation));
			}
		});

		ReflectionUtils.doWithMethods(beanClass, method -> {
			GrpcClient annotation = AnnotationUtils.findAnnotation(method, GrpcClient.class);
			if (annotation != null) {
				if (Modifier.isStatic(method.getModifiers())) {
					throw new IllegalStateException(
							"@GrpcClient annotation is not supported on static methods: " + method);
				}
				if (method.getParameterCount() != 1) {
					throw new IllegalStateException(
							"@GrpcClient annotation on methods requires exactly one parameter: " + method);
				}
				targets.add(new MethodInjectionTarget(method, annotation));
			}
		});

		return targets;
	}

	private Object createClient(InjectionTarget target) {
		Class<?> clientType = target.getClientType();
		GrpcClient annotation = target.getAnnotation();

		List<ClientInterceptor> interceptors = resolveInterceptors(annotation);
		ChannelBuilderOptions options = ChannelBuilderOptions.defaults()
			.withInterceptors(interceptors)
			.withInterceptorsMerge(annotation.blendWithGlobalInterceptors());

		if (Channel.class.isAssignableFrom(clientType)) {
			return getChannelFactory().createChannel(annotation.value(), options);
		}

		if (AbstractStub.class.isAssignableFrom(clientType)) {
			return getClientFactory().getClient(annotation.value(), clientType, UnspecifiedStubFactory.class, options);
		}

		throw new BeanInstantiationException(clientType,
				"Unsupported gRPC client type. Expected Channel or AbstractStub subclass.");
	}

	private List<ClientInterceptor> resolveInterceptors(GrpcClient annotation) {
		List<ClientInterceptor> interceptors = new ArrayList<>();
		ApplicationContext context = requireApplicationContext();

		for (Class<? extends ClientInterceptor> interceptorClass : annotation.interceptors()) {
			ClientInterceptor interceptor = resolveInterceptorByType(context, interceptorClass);
			interceptors.add(interceptor);
		}

		for (String interceptorName : annotation.interceptorNames()) {
			ClientInterceptor interceptor = context.getBean(interceptorName, ClientInterceptor.class);
			interceptors.add(interceptor);
		}

		return interceptors;
	}

	private ClientInterceptor resolveInterceptorByType(ApplicationContext context,
			Class<? extends ClientInterceptor> interceptorClass) {
		try {
			return context.getBean(interceptorClass);
		}
		catch (BeansException ex) {
			try {
				return interceptorClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception instantiationEx) {
				throw new BeanCreationException(
						"Failed to instantiate ClientInterceptor: " + interceptorClass.getName()
								+ ". Make sure it's available as a bean or has a no-args constructor.",
						instantiationEx);
			}
		}
	}

	private ApplicationContext requireApplicationContext() {
		Assert.state(this.applicationContext != null, "ApplicationContext has not been set");
		return this.applicationContext;
	}

	private GrpcClientFactory getClientFactory() {
		return requireApplicationContext().getBean(GrpcClientFactory.class);
	}

	private GrpcChannelFactory getChannelFactory() {
		return requireApplicationContext().getBean(GrpcChannelFactory.class);
	}

	/**
	 * Represents an injection target (field or method) annotated with {@link GrpcClient}.
	 */
	private interface InjectionTarget {

		Member getMember();

		Class<?> getClientType();

		GrpcClient getAnnotation();

		void inject(Object bean, Object client);

	}

	/**
	 * Injection target for fields annotated with {@link GrpcClient}.
	 */
	private static class FieldInjectionTarget implements InjectionTarget {

		private final Field field;

		private final GrpcClient annotation;

		FieldInjectionTarget(Field field, GrpcClient annotation) {
			this.field = field;
			this.annotation = annotation;
		}

		@Override
		public Member getMember() {
			return this.field;
		}

		@Override
		public Class<?> getClientType() {
			return this.field.getType();
		}

		@Override
		public GrpcClient getAnnotation() {
			return this.annotation;
		}

		@Override
		public void inject(Object bean, Object client) {
			ReflectionUtils.makeAccessible(this.field);
			ReflectionUtils.setField(this.field, bean, client);
		}

	}

	/**
	 * Injection target for setter methods annotated with {@link GrpcClient}.
	 */
	private static class MethodInjectionTarget implements InjectionTarget {

		private final Method method;

		private final GrpcClient annotation;

		MethodInjectionTarget(Method method, GrpcClient annotation) {
			this.method = method;
			this.annotation = annotation;
		}

		@Override
		public Member getMember() {
			return this.method;
		}

		@Override
		public Class<?> getClientType() {
			return this.method.getParameterTypes()[0];
		}

		@Override
		public GrpcClient getAnnotation() {
			return this.annotation;
		}

		@Override
		public void inject(Object bean, Object client) {
			ReflectionUtils.makeAccessible(this.method);
			ReflectionUtils.invokeMethod(this.method, bean, client);
		}

	}

}
