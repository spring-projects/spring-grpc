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
package org.springframework.grpc.client;

import io.grpc.ClientInterceptor;

/**
 * Strategy to determine whether a {@link ClientInterceptor} should be included for a
 * given {@link GrpcChannelFactory}.
 *
 * @author Andrey Litvitski
 */
@FunctionalInterface
public interface ClientInterceptorFilter {

	/**
	 * Determine whether the given {@link ClientInterceptor} should be included for the
	 * provided {@link GrpcChannelFactory}.
	 * @param interceptor the client interceptor under consideration.
	 * @param channelFactory the channel factory in use.
	 * @return {@code true} if the interceptor should be included; {@code false}
	 * otherwise.
	 */
	boolean filter(ClientInterceptor interceptor, GrpcChannelFactory channelFactory);

}
