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

package org.springframework.grpc.server.security;

import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
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
		return new SecurityContextHandlerListener<ReqT, RespT>(next.startCall(call, headers), securityContext);
	}

	static class SecurityContextHandlerListener<ReqT, RespT> extends SimpleForwardingServerCallListener<ReqT> {

		private SecurityContext securityContext;

		SecurityContextHandlerListener(ServerCall.Listener<ReqT> delegate, SecurityContext securityContext) {
			super(delegate);
			this.securityContext = securityContext;
		}

		@Override
		public void onMessage(ReqT message) {
			SecurityContextHolder.setContext(this.securityContext);
			try {
				super.onMessage(message);
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		}

		@Override
		public void onHalfClose() {
			SecurityContextHolder.setContext(this.securityContext);
			try {
				super.onHalfClose();
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		}

		@Override
		public void onReady() {
			SecurityContextHolder.setContext(this.securityContext);
			try {
				super.onReady();
			}
			finally {
				SecurityContextHolder.clearContext();
			}
		}

		@Override
		public void onCancel() {
			super.onCancel();
			SecurityContextHolder.clearContext();
		}

		@Override
		public void onComplete() {
			super.onComplete();
			SecurityContextHolder.clearContext();
		}

	}

}
