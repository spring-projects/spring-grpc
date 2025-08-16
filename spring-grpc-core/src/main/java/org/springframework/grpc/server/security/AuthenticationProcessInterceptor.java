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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

/**
 * An interceptor that extracts the authentication credentials from the gRPC request
 * headers and metadata, authenticates the user, and sets the authentication in the
 * SecurityContext. This interceptor should be registered with the gRPC server to handle
 * authentication and authorization for gRPC requests.
 *
 * @author Dave Syer
 * @author Hyunsang Han
 */
public class AuthenticationProcessInterceptor implements ServerInterceptor, Ordered {

	private final AuthenticationManager authenticationManager;

	private final GrpcAuthenticationExtractor extractor;

	private AuthorizationManager<CallContext> authorizationManager;

	@Override
	public int getOrder() {
		return GrpcSecurity.CONTEXT_FILTER_ORDER - 10;
	}

	public AuthenticationProcessInterceptor(AuthenticationManager authenticationManager,
			GrpcAuthenticationExtractor extractor, AuthorizationManager<CallContext> authorizationManager) {
		this.authenticationManager = authenticationManager;
		this.extractor = extractor;
		this.authorizationManager = authorizationManager;
	}

	@Override
	public <ReqT, RespT> Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
			ServerCallHandler<ReqT, RespT> next) {
		SecurityContext securityContext = SecurityContextHolder.getContext();
		Authentication user = this.extractor.extract(headers, call.getAttributes());
		if (user != null) {
			user = this.authenticationManager.authenticate(user);
			securityContext.setAuthentication(user);
		}

		if (this.authorizationManager != null) {
			CallContext context = new CallContext(headers, call.getAttributes(), call.getMethodDescriptor());
			if (user == null) {
				// Maybe just throw BadCredentialsException (authentication manager would
				// have to make the anonymous user)?
				user = new AnonymousAuthenticationToken("anonymous", "anonymous",
						AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
			}
			Authentication authentication = user;
			if (!this.authorizationManager.authorize(() -> authentication, context).isGranted()) {
				if (user instanceof AnonymousAuthenticationToken) {
					throw new BadCredentialsException("not authenticated");
				}
				throw new AccessDeniedException("not allowed");
			}
		}
		else if (user == null || !user.isAuthenticated()) {
			throw new BadCredentialsException("not authenticated");
		}

		SecurityContext currentContext = SecurityContextHolder.getContext();
		return new SecurityContextClearingListener<>(next.startCall(call, headers), currentContext);
	}

	static class SecurityContextClearingListener<ReqT> extends SimpleForwardingServerCallListener<ReqT> {

		private final SecurityContext securityContext;

		SecurityContextClearingListener(ServerCall.Listener<ReqT> delegate, SecurityContext securityContext) {
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
