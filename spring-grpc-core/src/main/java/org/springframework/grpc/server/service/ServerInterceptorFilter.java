/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
