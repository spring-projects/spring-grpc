/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.grpc.server.service;

import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;

/**
 * Strategy to determine whether a global {@link ServerInterceptor server interceptor}
 * should be applied to {@link ServerServiceDefinition gRPC service}.
 *
 * @author Chris Bono
 */
@FunctionalInterface
public interface ServerInterceptorFilter {

	/**
	 * Determine whether an interceptor should be applied to a service when the service is
	 * running on a server provided by the given server factory.
	 * @param interceptor the server interceptor under consideration.
	 * @param service the service being added.
	 * @return {@code true} if the interceptor should be included; {@code false}
	 * otherwise.
	 */
	boolean filter(ServerInterceptor interceptor, ServerServiceDefinition service);

}
