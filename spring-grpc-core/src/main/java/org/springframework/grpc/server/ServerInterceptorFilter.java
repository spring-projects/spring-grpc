/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.grpc.server;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;

/**
 * Strategy to determine whether a global {@link ServerInterceptor} should be included for
 * a given {@link BindableService service} and {@link GrpcServerFactory server factory}.
 *
 * @author Chris Bono
 */
@FunctionalInterface
public interface ServerInterceptorFilter {

	/**
	 * Determine whether the given {@link ServerInterceptor} should be included for the
	 * provided {@link BindableService service} and {@link GrpcServerFactory server
	 * factory}.
	 * @param interceptor the server interceptor under consideration.
	 * @param serverFactory the server factory in use.
	 * @param service the service being added.
	 * @return {@code true} if the interceptor should be included; {@code false}
	 * otherwise.
	 */
	boolean filter(ServerInterceptor interceptor, GrpcServerFactory serverFactory, BindableService service);

}
