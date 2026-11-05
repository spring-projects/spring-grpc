/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
