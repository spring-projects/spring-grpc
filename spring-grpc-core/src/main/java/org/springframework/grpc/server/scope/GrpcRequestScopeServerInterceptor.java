/*
 * Copyright 2024-present the original author or authors.
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
