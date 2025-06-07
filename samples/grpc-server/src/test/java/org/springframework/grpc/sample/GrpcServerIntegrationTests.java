/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.autoconfigure.server.GrpcServerProperties;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.test.AutoConfigureInProcessTransport;
import org.springframework.grpc.test.LocalGrpcPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.grpc.ForwardingServerCallListener;
import io.grpc.ManagedChannel;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerInterceptor;
import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.NettyChannelBuilder;

/**
 * More detailed integration tests for {@link GrpcServerFactory gRPC server factories} and
 * various {@link GrpcServerProperties}.
 */
class GrpcServerIntegrationTests {

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class ServerWithInProcessChannel {

		@Test
		void servesResponseToClient(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:0"));
		}

	}

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class ServerWithException {

		@Test
		void specificErrorResponse(@Autowired GrpcChannelFactory channels) {
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("error").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.INVALID_ARGUMENT);
		}

		@Test
		void defaultErrorResponseIsUnknown(@Autowired GrpcChannelFactory channels) {
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("internal").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.UNKNOWN);
		}

	}

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class ServerWithExceptionInInterceptorCall {

		@Test
		void specificErrorResponse(@Autowired GrpcChannelFactory channels) {
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("foo").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.INVALID_ARGUMENT);
		}

		@TestConfiguration
		static class TestConfig {

			@Bean
			@GlobalServerInterceptor
			public ServerInterceptor exceptionInterceptor() {
				return new CustomInterceptor();
			}

			static class CustomInterceptor implements ServerInterceptor {

				@Override
				public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
						io.grpc.ServerCall<ReqT, RespT> call, io.grpc.Metadata headers,
						io.grpc.ServerCallHandler<ReqT, RespT> next) {
					throw new IllegalArgumentException("test");

				}

			}

		}

	}

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class ServerWithExceptionInInterceptorListener {

		@Test
		void specificErrorResponse(@Autowired GrpcChannelFactory channels) {
			TestConfig.reset();
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("foo").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.INVALID_ARGUMENT);
			assertThat(TestConfig.readyCount.get()).isEqualTo(1);
			assertThat(TestConfig.callCount.get()).isEqualTo(0);
			assertThat(TestConfig.messageCount.get()).isEqualTo(0);
		}

		@TestConfiguration
		static class TestConfig {

			static AtomicInteger callCount = new AtomicInteger();
			static AtomicInteger messageCount = new AtomicInteger();
			static AtomicInteger readyCount = new AtomicInteger();

			@Bean
			@GlobalServerInterceptor
			public ServerInterceptor exceptionInterceptor() {
				return new CustomInterceptor();
			}

			public static void reset() {
				callCount.set(0);
				messageCount.set(0);
				readyCount.set(0);
			}

			static class CustomInterceptor implements ServerInterceptor {

				@Override
				public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
						io.grpc.ServerCall<ReqT, RespT> call, io.grpc.Metadata headers,
						io.grpc.ServerCallHandler<ReqT, RespT> next) {
					return new CustomListener<>(next.startCall(call, headers));
				}

			}

			static class CustomListener<ReqT> extends ForwardingServerCallListener<ReqT> {

				private Listener<ReqT> delegate;

				CustomListener(io.grpc.ServerCall.Listener<ReqT> delegate) {
					this.delegate = delegate;
				}

				@Override
				public void onReady() {
					readyCount.incrementAndGet();
					throw new IllegalArgumentException("test");
				}

				@Override
				public void onHalfClose() {
					callCount.incrementAndGet();
					super.onHalfClose();
				}

				@Override
				public void onMessage(ReqT message) {
					messageCount.incrementAndGet();
					super.onMessage(message);
				}

				@Override
				protected Listener<ReqT> delegate() {
					return this.delegate;
				}

			}

		}

	}

	@Nested
	@SpringBootTest("spring.grpc.server.exception-handler.enabled=false")
	@AutoConfigureInProcessTransport
	class ServerWithUnhandledException {

		@Test
		void specificErrorResponse(@Autowired GrpcChannelFactory channels) {
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("error").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.UNKNOWN);
		}

		@Test
		void defaultErrorResponseIsUnknown(@Autowired GrpcChannelFactory channels) {
			SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"));
			assertThat(assertThrows(StatusRuntimeException.class,
					() -> client.sayHello(HelloRequest.newBuilder().setName("internal").build()))
				.getStatus()
				.getCode()).isEqualTo(Code.UNKNOWN);
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=0.0.0.0", "spring.grpc.server.port=0" })
	class ServerWithAnyIPv4AddressAndRandomPort {

		@Test
		void servesResponseToClientWithAnyIPv4AddressAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=::", "spring.grpc.server.port=0" })
	class ServerWithAnyIPv6AddressAndRandomPort {

		@Test
		void servesResponseToClientWithAnyIPv4AddressAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.host=127.0.0.1", "spring.grpc.server.port=0" })
	class ServerWithLocalhostAndRandomPort {

		@Test
		void servesResponseToClientWithLocalhostAndRandomPort(@Autowired GrpcChannelFactory channels,
				@LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("127.0.0.1:" + port));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0",
			"spring.grpc.client.channels.test-channel.address=static://0.0.0.0:${local.grpc.port}" })
	@DirtiesContext
	class ServerConfiguredWithStaticClientChannel {

		@Test
		void servesResponseToClientWithConfiguredChannel(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.address=unix:unix-test-channel" })
	@EnabledOnOs(OS.LINUX)
	class ServerWithUnixDomain {

		@Test
		void clientChannelWithUnixDomain(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("unix:unix-test-channel",
					ChannelBuilderOptions.defaults().<NettyChannelBuilder>withCustomizer((__, b) -> b.usePlaintext())));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0",
			"spring.grpc.client.channels.test-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.channels.test-channel.negotiation-type=TLS",
			"spring.grpc.client.channels.test-channel.secure=false" })
	@ActiveProfiles("ssl")
	@DirtiesContext
	class ServerWithSsl {

		@Test
		void clientChannelWithSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.port=0", "spring.grpc.server.ssl.client-auth=REQUIRE",
			"spring.grpc.server.ssl.secure=false",
			"spring.grpc.client.channels.test-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.channels.test-channel.ssl.bundle=ssltest",
			"spring.grpc.client.channels.test-channel.negotiation-type=TLS",
			"spring.grpc.client.channels.test-channel.secure=false" })
	@ActiveProfiles("ssl")
	@DirtiesContext
	class ServerWithClientAuth {

		@Test
		void clientChannelWithSsl(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("test-channel"));
		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.server.inprocess.name=foo", "spring.grpc.server.host=0.0.0.0",
			"spring.grpc.server.port=0" })
	class ServerWithRegularAndInProcessChannelsAndFactories {

		@Test
		void servesResponseToNonInProcessClient(@Autowired GrpcChannelFactory channels, @LocalGrpcPort int port) {
			assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port));
		}

		@Test
		void servesResponseToInProcessClient(@Autowired GrpcChannelFactory channels) {
			assertThatResponseIsServedToChannel(channels.createChannel("in-process:foo"));
		}

	}

	private void assertThatResponseIsServedToChannel(ManagedChannel clientChannel) {
		SimpleGrpc.SimpleBlockingStub client = SimpleGrpc.newBlockingStub(clientChannel);
		HelloReply response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertThat(response.getMessage()).isEqualTo("Hello ==> Alien");
	}

}
