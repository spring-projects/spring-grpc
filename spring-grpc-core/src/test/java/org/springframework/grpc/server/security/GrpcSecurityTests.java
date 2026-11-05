/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
