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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;

import com.google.protobuf.Empty;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.protobuf.ProtoUtils;
import io.grpc.stub.ClientCalls;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.grpc.ObservationGrpcServerInterceptor;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;

/**
 * Ensures {@link AuthenticationProcessInterceptor} sits inside
 * {@link GrpcExceptionHandlerInterceptor} so {@link SecurityGrpcExceptionHandler} can map
 * auth failures (and Micrometer can record the mapped status).
 *
 * @author Yinghuai Fu
 */
class AuthenticationProcessInterceptorExceptionHandlerTests {

	private static final String SERVICE_NAME = "test.SecureObservationService";

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "Get"))
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	@Test
	void authenticationInterceptorOrderIsInsideExceptionHandler() {
		AuthenticationProcessInterceptor auth = unauthenticatedInterceptor();
		assertThat(auth.getOrder()).isGreaterThan(GrpcExceptionHandlerInterceptor.ORDER);
		assertThat(new SecurityContextServerInterceptor().getOrder()).isGreaterThan(auth.getOrder());
	}

	@Test
	void unauthenticatedCallMapsToUnauthenticatedAndObservationStatus() throws Exception {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));

		ObservationGrpcServerInterceptor observationInterceptor = new ObservationGrpcServerInterceptor(
				observationRegistry);
		GrpcExceptionHandlerInterceptor exceptionInterceptor = new GrpcExceptionHandlerInterceptor(
				new SecurityGrpcExceptionHandler());
		AuthenticationProcessInterceptor authInterceptor = unauthenticatedInterceptor();

		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					call.sendHeaders(new io.grpc.Metadata());
					call.sendMessage(Empty.getDefaultInstance());
					call.close(Status.OK, new io.grpc.Metadata());
				}
			};
		};

		ServerServiceDefinition service = ServerServiceDefinition.builder(SERVICE_NAME)
			.addMethod(METHOD, handler)
			.build();

		// Mimic Spring's orderedStream() + interceptForward(): lower order is outer.
		List<ServerInterceptor> interceptors = new ArrayList<>();
		interceptors.add(authInterceptor);
		interceptors.add(exceptionInterceptor);
		interceptors.add(new OrderedObservationInterceptor(observationInterceptor));
		AnnotationAwareOrderComparator.sort(interceptors);
		ServerServiceDefinition intercepted = ServerInterceptors.interceptForward(service, interceptors);

		String serverName = InProcessServerBuilder.generateName();
		Server server = InProcessServerBuilder.forName(serverName)
			.directExecutor()
			.addService(intercepted)
			.build()
			.start();
		ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
		try {
			CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS);
			assertThatThrownBy(
					() -> ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance()))
				.isInstanceOf(StatusRuntimeException.class)
				.extracting(ex -> ((StatusRuntimeException) ex).getStatus().getCode())
				.isEqualTo(Status.Code.UNAUTHENTICATED);

			String meters = meterRegistry.getMeters()
				.stream()
				.map(meter -> meter.getId().toString())
				.collect(Collectors.joining(", "));
			Timer byStatus = meterRegistry.find("grpc.server").tag("grpc.status", "UNAUTHENTICATED").timer();
			Timer byStatusCode = meterRegistry.find("grpc.server").tag("grpc.status_code", "UNAUTHENTICATED").timer();
			assertThat(byStatus != null || byStatusCode != null).as("meters=%s", meters).isTrue();
		}
		finally {
			channel.shutdownNow();
			server.shutdownNow();
			channel.awaitTermination(2, TimeUnit.SECONDS);
			server.awaitTermination(2, TimeUnit.SECONDS);
		}
	}

	private static AuthenticationProcessInterceptor unauthenticatedInterceptor() {
		return new AuthenticationProcessInterceptor((authentication) -> {
			throw new AssertionError("extractor returned no credentials");
		}, (headers, attributes, method) -> null, null);
	}

	/**
	 * Boots registers {@link ObservationGrpcServerInterceptor} as {@code @Order(0)}; the
	 * Micrometer class itself is not {@link org.springframework.core.Ordered}.
	 */
	private static final class OrderedObservationInterceptor
			implements ServerInterceptor, org.springframework.core.Ordered {

		private final ObservationGrpcServerInterceptor delegate;

		private OrderedObservationInterceptor(ObservationGrpcServerInterceptor delegate) {
			this.delegate = delegate;
		}

		@Override
		public int getOrder() {
			return 0;
		}

		@Override
		public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
				io.grpc.Metadata headers, ServerCallHandler<ReqT, RespT> next) {
			return this.delegate.interceptCall(call, headers, next);
		}

	}

}
