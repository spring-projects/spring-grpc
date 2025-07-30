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

import java.util.Base64;

import org.springframework.grpc.server.security.GrpcSecurity;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.MethodDescriptor;

/**
 * An interceptor that adds basic authentication headers to gRPC client calls. This
 * interceptor is used to authenticate gRPC client calls with a username and password. The
 * username and password are encoded using Base64 and added to the Authorization header.
 *
 * @author Dave Syer
 */
public class BasicAuthenticationInterceptor implements ClientInterceptor {

	private final String username;

	private final String password;

	public BasicAuthenticationInterceptor(String username, String password) {
		this.username = username;
		this.password = password;
	}

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
			CallOptions callOptions, Channel next) {
		return new SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
			public void start(ClientCall.Listener<RespT> responseListener, io.grpc.Metadata headers) {
				headers.put(GrpcSecurity.AUTHORIZATION_KEY,
						"Basic " + Base64.getEncoder()
							.encodeToString((BasicAuthenticationInterceptor.this.username + ":"
									+ BasicAuthenticationInterceptor.this.password)
								.getBytes()));
				super.start(responseListener, headers);
			}
		};
	}

}
