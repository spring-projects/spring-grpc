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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.Scope;

/**
 * A {@link Scope} implementation for gRPC request-scoped beans.
 *
 * @author Fernando Rodrigues Ripol
 */
public class GrpcRequestScope implements Scope {

	private static final ThreadLocal<Map<String, Object>> threadLocalScope = ThreadLocal.withInitial(HashMap::new);

	@Override
	public Object get(String name, ObjectFactory<?> objectFactory) {
		Map<String, Object> scope = threadLocalScope.get();
		Object bean = scope.get(name);
		if (bean == null) {
			bean = objectFactory.getObject();
			scope.put(name, bean);
		}
		return bean;
	}

	@Override
	public Object remove(String name) {
		Map<String, Object> scope = threadLocalScope.get();
		return scope.remove(name);
	}

	@Override
	public void registerDestructionCallback(String name, Runnable callback) {
	}

	@Override
	public Object resolveContextualObject(String key) {
		return null;
	}

	@Override
	public String getConversationId() {
		return null;
	}

	public static void clear() {
		threadLocalScope.remove();
	}

}
