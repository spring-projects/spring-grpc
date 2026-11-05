/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server;

import io.grpc.ServerServiceDefinition;

/**
 * Strategy to determine whether a {@link ServerServiceDefinition} should be added to a
 * {@link GrpcServerFactory server factory}.
 *
 * @author Andrey Litvitski
 */
@FunctionalInterface
public interface ServerServiceDefinitionFilter {

	/**
	 * Determine whether the given {@link ServerServiceDefinition} should be added to the
	 * given {@link GrpcServerFactory server factory}.
	 * @param serviceDefinition the gRPC service definition under consideration.
	 * @param serverFactory the server factory in use.
	 * @return {@code true} if the service should be included; {@code false} otherwise.
	 */
	boolean filter(ServerServiceDefinition serviceDefinition, GrpcServerFactory serverFactory);

}
