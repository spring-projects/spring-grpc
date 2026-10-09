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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.protobuf.Empty;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
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
 * Verifies that exceptions mapped by {@link GrpcExceptionHandlerInterceptor} are
 * reflected in Micrometer {@code grpc.server} observations when the exception handler
 * sits inside {@link ObservationGrpcServerInterceptor}.
 *
 * @author Yinghuai Fu
 * @see <a href="https://github.com/spring-projects/spring-grpc/issues/438">gh-438</a>
 */
class GrpcExceptionHandlerObservationTests {

	private static final String SERVICE_NAME = "test.ObservationService";

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "Get"))
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	@Test
	void handledExceptionRecordsMappedStatusOnServerObservation() throws Exception {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		observationRegistry.observationConfig().observationHandler(new DefaultMeterObservationHandler(meterRegistry));

		ObservationGrpcServerInterceptor observationInterceptor = new ObservationGrpcServerInterceptor(
				observationRegistry);
		GrpcExceptionHandlerInterceptor exceptionInterceptor = new GrpcExceptionHandlerInterceptor(
				ex -> ex instanceof NoSuchElementException ? Status.NOT_FOUND.withDescription("not found").asException()
						: null);

		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					throw new NoSuchElementException("missing");
				}
			};
		};

		ServerServiceDefinition service = ServerServiceDefinition.builder(SERVICE_NAME)
			.addMethod(METHOD, handler)
			.build();
		// Observation (@Order(0)) outside exception handler (@Order(1)).
		ServerServiceDefinition intercepted = ServerInterceptors.interceptForward(service, observationInterceptor,
				exceptionInterceptor);

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
				.isEqualTo(Status.Code.NOT_FOUND);

			String meters = meterRegistry.getMeters()
				.stream()
				.map(meter -> meter.getId().toString())
				.collect(Collectors.joining(", "));
			Timer byStatus = meterRegistry.find("grpc.server").tag("grpc.status", "NOT_FOUND").timer();
			Timer byStatusCode = meterRegistry.find("grpc.server").tag("grpc.status_code", "NOT_FOUND").timer();
			assertThat(byStatus != null || byStatusCode != null).as("meters=%s", meters).isTrue();
		}
		finally {
			channel.shutdownNow();
			server.shutdownNow();
			channel.awaitTermination(2, TimeUnit.SECONDS);
			server.awaitTermination(2, TimeUnit.SECONDS);
		}
	}

}
