/*
 * Copyright 2025-present the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;

import com.google.protobuf.Empty;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.protobuf.ProtoUtils;

/**
 * Tests for {@link GrpcSecurity}.
 */
class GrpcSecurityTests {

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName("Simple/SayHello")
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	@Test
	void anonymousCallIsServedWhenNoExtractorIsRegistered() throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			GrpcSecurity grpc = new GrpcSecurity(ObjectPostProcessor.identity(),
					new AuthenticationManagerBuilder(ObjectPostProcessor.identity()), context);
			grpc.authorizeRequests((requests) -> requests.allRequests().permitAll());

			AuthenticationProcessInterceptor interceptor = grpc.build();

			@SuppressWarnings("unchecked")
			ServerCall<Empty, Empty> call = mock(ServerCall.class);
			when(call.getAttributes()).thenReturn(Attributes.EMPTY);
			when(call.getMethodDescriptor()).thenReturn(METHOD);
			@SuppressWarnings("unchecked")
			ServerCallHandler<Empty, Empty> next = mock(ServerCallHandler.class);
			@SuppressWarnings("unchecked")
			ServerCall.Listener<Empty> listener = mock(ServerCall.Listener.class);
			when(next.startCall(any(), any())).thenReturn(listener);

			assertThat(interceptor.interceptCall(call, new Metadata(), next)).isNotNull();
		}
	}

}
