/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import org.jspecify.annotations.Nullable;

import org.springframework.grpc.server.GrpcServerFactory;

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
