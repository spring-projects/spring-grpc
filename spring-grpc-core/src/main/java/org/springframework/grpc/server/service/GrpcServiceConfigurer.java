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

import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.lang.Nullable;

import io.grpc.ServerServiceDefinition;

/**
 * Configures and binds a {@link GrpcServiceSpec service spec} into a
 * {@link ServerServiceDefinition service definition} that can then be added to a gRPC
 * server.
 *
 * @author Chris Bono
 */
@FunctionalInterface
public interface GrpcServiceConfigurer {

	/**
	 * Configure and bind a gRPC server spec resulting in a service definition that can
	 * then be added to a gRPC server.
	 * @param serviceSpec the spec containing the info about the service
	 * @param serverFactory the factory that can be used to create a gRPC server, or
	 * {@code null} if the service is not bound to a specific server factory.
	 * @return bound and configured service definition that is ready to be added to a
	 * server
	 */
	ServerServiceDefinition configure(GrpcServiceSpec serviceSpec, @Nullable GrpcServerFactory serverFactory);

}
