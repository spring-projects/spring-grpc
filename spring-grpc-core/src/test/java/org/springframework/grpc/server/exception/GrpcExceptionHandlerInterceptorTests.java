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

package org.springframework.grpc.server.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor.FallbackHandler;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.StatusException;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.protobuf.StatusProto;

/**
 * Tests for {@link GrpcExceptionHandlerInterceptor}.
 *
 * @author Dave Syer
 * @author Andrey Litvitski
 */
public class GrpcExceptionHandlerInterceptorTests {

	@Test
	void testNullStatusHandled() {
		assertThat(new FallbackHandler(exception -> null).handleException(new RuntimeException("Test exception")))
			.isNotNull();
	}

	@Test
	void propagatesTrailersFromStatusExceptionWhenStartCallThrows() {
		Status statusWithDetails = Status.newBuilder()
			.setCode(Code.PERMISSION_DENIED_VALUE)
			.setMessage("access denied")
			.addDetails(Any.pack(Empty.getDefaultInstance()))
			.build();
		StatusException statusEx = StatusProto.toStatusException(statusWithDetails);
		GrpcExceptionHandler handler = ex -> statusEx;
		ServerInterceptor interceptor = new GrpcExceptionHandlerInterceptor(handler);
		@SuppressWarnings("unchecked")
		ServerCall<Empty, Empty> call = mock(ServerCall.class);
		MethodDescriptor<Empty, Empty> method = MethodDescriptor.<Empty, Empty>newBuilder()
			.setType(MethodDescriptor.MethodType.UNARY)
			.setFullMethodName("test/Test")
			.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
			.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
			.build();
		when(call.getMethodDescriptor()).thenReturn(method);
		ServerCallHandler<Empty, Empty> next = (c, headers) -> {
			throw new RuntimeException("boom");
		};
		interceptor.interceptCall(call, new Metadata(), next);
		ArgumentCaptor<io.grpc.Status> statusCaptor = ArgumentCaptor.forClass(io.grpc.Status.class);
		ArgumentCaptor<Metadata> trailersCaptor = ArgumentCaptor.forClass(Metadata.class);
		verify(call, times(1)).close(statusCaptor.capture(), trailersCaptor.capture());
		io.grpc.Status closedStatus = statusCaptor.getValue();
		Metadata closedTrailers = trailersCaptor.getValue();
		assertThat(closedStatus.getCode()).isEqualTo(io.grpc.Status.Code.PERMISSION_DENIED);
		Status extracted = StatusProto.fromThrowable(new StatusException(closedStatus, closedTrailers));
		assertThat(extracted).isNotNull();
		assertThat(extracted).isEqualTo(statusWithDetails);
	}

}
