/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class SecurityContextServerInterceptor implements ServerInterceptor, Ordered {

	@Override
	public int getOrder() {
		return GrpcSecurity.CONTEXT_FILTER_ORDER;
	}

	@Override
	public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Context context = Context.current().withValue(GrpcSecurity.SECURITY_CONTEXT_KEY, securityContext);
		return new SecurityContextHandlerListener<ReqT, RespT>(Contexts.interceptCall(context, call, headers, next),
				securityContext);
	}

}
