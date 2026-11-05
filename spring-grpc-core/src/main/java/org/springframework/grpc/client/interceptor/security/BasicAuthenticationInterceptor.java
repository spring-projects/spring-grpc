/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client.interceptor.security;

import java.util.Base64;

import org.springframework.grpc.internal.GrpcHeaders;

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
		return new SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
			public void start(ClientCall.Listener<RespT> responseListener, io.grpc.Metadata headers) {
				headers.put(GrpcHeaders.AUTHORIZATION_KEY,
						"Basic " + Base64.getEncoder()
							.encodeToString((BasicAuthenticationInterceptor.this.username + ":"
									+ BasicAuthenticationInterceptor.this.password)
								.getBytes()));
				super.start(responseListener, headers);
			}
		};
	}

}
