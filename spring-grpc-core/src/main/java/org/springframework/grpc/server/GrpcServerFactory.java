/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
