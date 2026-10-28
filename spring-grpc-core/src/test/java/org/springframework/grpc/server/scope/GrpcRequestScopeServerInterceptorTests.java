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

package org.springframework.grpc.server.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.annotation.GrpcRequestScope;
import org.springframework.grpc.server.service.DefaultGrpcServiceConfigurer;
import org.springframework.grpc.server.service.GrpcServiceSpec;

import com.google.protobuf.Empty;
import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
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

/**
 * Tests for {@link GrpcRequestScopeServerInterceptor} and the scope it opens, activated
 * with {@link EnableGrpcScope @EnableGrpcScope}.
 */
class GrpcRequestScopeServerInterceptorTests {

	private static final String SERVICE_NAME = "test.ScopedService";

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName(MethodDescriptor.generateFullMethodName(SERVICE_NAME, "Get"))
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	private final List<String> observedIds = new CopyOnWriteArrayList<>();

	private final ExecutorService worker = Executors.newSingleThreadExecutor();

	private AnnotationConfigApplicationContext context;

	private CallState callState;

	@BeforeEach
	void createContext() {
		CallState.destroyed.clear();
		this.context = new AnnotationConfigApplicationContext(ScopeConfiguration.class);
		this.callState = this.context.getBean(CallState.class);
	}

	@AfterEach
	void closeContext() {
		this.context.close();
		this.worker.shutdownNow();
	}

	@Test
	void scopedBeanIsCreatedPerCallAndDestroyedWhenTheCallCompletes() throws Exception {
		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					observedIds.add(callState.getId());
					observedIds.add(callState.getId());
					call.sendHeaders(new Metadata());
					call.sendMessage(Empty.getDefaultInstance());
					call.close(Status.OK, new Metadata());
				}
			};
		};
		withServer(handler, (channel) -> {
			CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS);
			ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance());
			ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance());
		});
		assertThat(this.observedIds).hasSize(4);
		assertThat(this.observedIds.get(1)).isEqualTo(this.observedIds.get(0));
		assertThat(this.observedIds.get(3)).isEqualTo(this.observedIds.get(2)).isNotEqualTo(this.observedIds.get(0));
		Awaitility.await()
			.atMost(Duration.ofSeconds(5))
			.untilAsserted(() -> assertThat(CallState.destroyed).containsExactlyInAnyOrder(this.observedIds.get(0),
					this.observedIds.get(2)));
	}

	@Test
	void scopedBeanIsDestroyedWhenTheCallIsCancelled() throws Exception {
		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					observedIds.add(callState.getId());
					// never respond, so the client's deadline cancels the call
				}
			};
		};
		withServer(handler, (channel) -> {
			CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(200, TimeUnit.MILLISECONDS);
			assertThatThrownBy(
					() -> ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance()))
				.isInstanceOf(StatusRuntimeException.class);
		});
		assertThat(this.observedIds).hasSize(1);
		Awaitility.await()
			.atMost(Duration.ofSeconds(5))
			.untilAsserted(() -> assertThat(CallState.destroyed).containsExactly(this.observedIds.get(0)));
	}

	@Test
	void scopedBeanIsSharedWithAThreadTheCallHandsWorkTo() throws Exception {
		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					observedIds.add(callState.getId());
					try {
						worker.submit(Context.current().wrap(() -> observedIds.add(callState.getId())))
							.get(5, TimeUnit.SECONDS);
					}
					catch (Exception ex) {
						throw new IllegalStateException(ex);
					}
					call.sendHeaders(new Metadata());
					call.sendMessage(Empty.getDefaultInstance());
					call.close(Status.OK, new Metadata());
				}
			};
		};
		withServer(handler, (channel) -> {
			CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS);
			ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance());
		});
		assertThat(this.observedIds).hasSize(2);
		assertThat(this.observedIds.get(1)).isEqualTo(this.observedIds.get(0));
		// Outside of the call, the worker thread has no scope either
		assertThatThrownBy(() -> this.worker.submit(this.callState::getId).get(5, TimeUnit.SECONDS))
			.hasCauseInstanceOf(ScopeNotActiveException.class);
	}

	@Test
	void scopeIsOpenForTheOtherGlobalInterceptors() throws Exception {
		ServerCallHandler<Empty, Empty> handler = (call, headers) -> {
			call.request(1);
			return new ServerCall.Listener<>() {
				@Override
				public void onHalfClose() {
					observedIds.add(callState.getId());
					call.sendHeaders(new Metadata());
					call.sendMessage(Empty.getDefaultInstance());
					call.close(Status.OK, new Metadata());
				}
			};
		};
		BindableService service = () -> ServerServiceDefinition.builder(SERVICE_NAME)
			.addMethod(METHOD, handler)
			.build();
		DefaultGrpcServiceConfigurer configurer = new DefaultGrpcServiceConfigurer(this.context);
		configurer.afterPropertiesSet();
		ServerServiceDefinition configured = configurer.configure(new GrpcServiceSpec(service, null), null);
		withService(configured, (channel) -> {
			CallOptions options = CallOptions.DEFAULT.withDeadlineAfter(5, TimeUnit.SECONDS);
			ClientCalls.blockingUnaryCall(channel, METHOD, options, Empty.getDefaultInstance());
		});
		ScopeProbeInterceptor probe = this.context.getBean(ScopeProbeInterceptor.class);
		assertThat(probe.seenIds).containsExactlyElementsOf(this.observedIds);
	}

	@Test
	void enableGrpcScopeRegistersTheInterceptorAsTheFirstGlobalInterceptor() {
		List<ServerInterceptor> globalInterceptors = ApplicationContextBeanLookupUtils
			.getBeansWithAnnotation(this.context, ServerInterceptor.class, GlobalServerInterceptor.class);
		assertThat(globalInterceptors).hasSize(2);
		assertThat(globalInterceptors.get(0)).isInstanceOf(GrpcRequestScopeServerInterceptor.class);
	}

	@Test
	void scopeIsNotActiveOutsideOfACall() {
		assertThatThrownBy(this.callState::getId).isInstanceOf(ScopeNotActiveException.class);
	}

	private void withServer(ServerCallHandler<Empty, Empty> handler, ChannelCallback callback) throws Exception {
		ServerServiceDefinition service = ServerServiceDefinition.builder(SERVICE_NAME)
			.addMethod(METHOD, handler)
			.build();
		GrpcRequestScopeServerInterceptor scope = this.context.getBean(GrpcRequestScopeServerInterceptor.class);
		withService(ServerInterceptors.interceptForward(service, scope), callback);
	}

	private void withService(ServerServiceDefinition service, ChannelCallback callback) throws Exception {
		String serverName = InProcessServerBuilder.generateName();
		Server server = InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build().start();
		ManagedChannel channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
		try {
			callback.call(channel);
		}
		finally {
			channel.shutdownNow();
			server.shutdownNow();
		}
	}

	@FunctionalInterface
	private interface ChannelCallback {

		void call(ManagedChannel channel) throws Exception;

	}

	@Configuration(proxyBeanMethods = false)
	@EnableGrpcScope
	static class ScopeConfiguration {

		@Bean
		@GlobalServerInterceptor
		ScopeProbeInterceptor scopeProbeInterceptor(CallState callState) {
			return new ScopeProbeInterceptor(callState);
		}

		@Bean
		@GrpcRequestScope
		CallState callState() {
			return new CallState();
		}

	}

	/**
	 * A global interceptor with the default (lowest) precedence that reads the scoped
	 * bean of the call it intercepts.
	 */
	static class ScopeProbeInterceptor implements ServerInterceptor {

		final List<String> seenIds = new CopyOnWriteArrayList<>();

		private final CallState callState;

		ScopeProbeInterceptor(CallState callState) {
			this.callState = callState;
		}

		@Override
		public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
				ServerCallHandler<ReqT, RespT> next) {
			this.seenIds.add(this.callState.getId());
			return next.startCall(call, headers);
		}

	}

	static class CallState implements DisposableBean {

		static final List<String> destroyed = new CopyOnWriteArrayList<>();

		private final String id = UUID.randomUUID().toString();

		public String getId() {
			return this.id;
		}

		@Override
		public void destroy() {
			destroyed.add(this.id);
		}

	}

}
