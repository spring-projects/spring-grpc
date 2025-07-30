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
 *
 * Partial copy from net.devh:grpc-spring-boot-starter.
 */

package org.springframework.grpc.server;

import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import io.grpc.Server;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;

/**
 * Factory interface that can be used to create a {@link Server gRPC Server}.
 *
 * @author David Syer
 * @author Chris Bono
 */
public interface GrpcServerFactory {

	/**
	 * Gets a new fully configured but not started {@link Server} instance. Clients should
	 * not be able to connect to the returned server until {@link Server#start()} is
	 * called (which happens when the {@code GrpcServerLifecycle} is started).
	 * @return a fully configured not started {@link Server} or null if the server has no
	 * lifecycle
	 * @see GrpcServerLifecycle
	 */
	Server createServer();

	/**
	 * Adds a service definition to the server. Must be called prior to
	 * {@link Server#start()}.
	 * @param service the service definition to add
	 */
	void addService(ServerServiceDefinition service);

	default boolean supports(ServerInterceptor interceptor, ServerServiceDefinition service) {
		return true;
	}

}
