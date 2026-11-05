/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.scope;

import org.springframework.core.Ordered;
import org.springframework.grpc.server.GlobalServerInterceptor;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * A global server interceptor that opens a new {@link GrpcRequestScope} for every call it
 * intercepts, ahead of the other interceptors in the service chain, so that request
 * scoped beans are available to them. The beans of the call are destroyed when the call
 * completes or is cancelled.
 *
 * @author Fernando Rodrigues Ripol
 * @author Sharang Gupta
 * @see EnableGrpcScope
 */
@GlobalServerInterceptor
public class GrpcRequestScopeServerInterceptor implements ServerInterceptor, Ordered {

	/**
	 * Runs before the other interceptors in the chain so that scoped beans are available
	 * to them.
	 */
	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		return Contexts.interceptCall(GrpcRequestScope.newCallContext(Context.current()), call, headers, next);
	}

}
