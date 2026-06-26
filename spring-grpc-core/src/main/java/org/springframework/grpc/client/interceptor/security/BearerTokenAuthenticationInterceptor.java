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

package org.springframework.grpc.client.interceptor.security;

import java.util.function.Supplier;

import org.springframework.grpc.internal.GrpcHeaders;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.MethodDescriptor;

/**
 * An interceptor that adds bearer token authentication headers to gRPC client calls.
 *
 * @author Dave Syer
 */
public class BearerTokenAuthenticationInterceptor implements ClientInterceptor {

	private final Supplier<String> token;

	public BearerTokenAuthenticationInterceptor(String token) {
		this(new TokenSupplier() {
			@Override
			public String token() {
				return token;
			}
		});
	}

	public BearerTokenAuthenticationInterceptor(Supplier<String> token) {
		this.token = token;
	}

	public BearerTokenAuthenticationInterceptor(TokenSupplier token) {
		this.token = () -> token.token();
	}

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {
		return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
			public void start(ClientCall.Listener<RespT> responseListener, io.grpc.Metadata headers) {
				headers.put(GrpcHeaders.AUTHORIZATION_KEY,
						"Bearer " + BearerTokenAuthenticationInterceptor.this.token.get());
				super.start(responseListener, headers);
			}
		};
	}

}
