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

package org.springframework.grpc.server.service;

import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * Configures and binds a {@link BindableService gRPC Service}.
 *
 * @author Chris Bono
 */
@FunctionalInterface
public interface GrpcServiceConfigurer {

	/**
	 * Configure and bind a gRPC service.
	 * @param serverFactory the factory that provides the server the service will be added
	 * to
	 * @param bindableService service to bind and configure
	 * @param serviceInfo optional additional service information
	 * @return configured service definition
	 */
	ServerServiceDefinition configure(GrpcServerFactory serverFactory, BindableService bindableService,
			@Nullable GrpcServiceInfo serviceInfo);

}
