/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
	 * @return {@code true} if the interceptor should be included; {@code false}
	 * otherwise.
	 */
	boolean filter(ClientInterceptor interceptor);

}
