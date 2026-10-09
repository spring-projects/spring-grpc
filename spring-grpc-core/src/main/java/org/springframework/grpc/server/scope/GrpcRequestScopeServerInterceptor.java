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

import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * A {@link ServerInterceptor} to manage the lifecycle of gRPC request-scoped beans.
 *
 * @author Fernando Rodrigues Ripol
 */
public class GrpcRequestScopeServerInterceptor implements ServerInterceptor {

	@Override
	public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
			ServerCall<ReqT, RespT> call,
			Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		ServerCall.Listener<ReqT> delegate = next.startCall(call, headers);
		return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
			@Override
			public void onComplete() {
				try {
					super.onComplete();
				}
				finally {
					GrpcRequestScope.clear();
				}
			}

			@Override
			public void onCancel() {
				try {
					super.onCancel();
				}
				finally {
					GrpcRequestScope.clear();
				}
			}
		};
	}

}
