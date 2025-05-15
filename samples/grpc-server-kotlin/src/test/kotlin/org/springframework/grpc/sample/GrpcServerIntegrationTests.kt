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
package org.springframework.grpc.sample

import io.grpc.ForwardingServerCallListener
import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.netty.NettyChannelBuilder
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.grpc.client.ChannelBuilderOptions
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpc
import org.springframework.grpc.server.GlobalServerInterceptor
import org.springframework.grpc.test.AutoConfigureInProcessTransport
import org.springframework.grpc.test.LocalGrpcPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.atomic.AtomicInteger

/**
 * More detailed integration tests for [gRPC server factories][GrpcServerFactory] and
 * various [GrpcServerProperties].
 */

@SpringBootTest
@AutoConfigureInProcessTransport
class ServerWithInProcessChannel {
    @Test
    fun servesResponseToClient(@Autowired channels: GrpcChannelFactory) {
        assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:0"))
    }
}


@SpringBootTest
@AutoConfigureInProcessTransport
class ServerWithException {

    @Test
    fun specificErrorResponse(@Autowired channels: GrpcChannelFactory) {
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat(
            Assert.assertThrows(StatusRuntimeException::class.java) {
                client.sayHello(
                    HelloRequest.newBuilder().setName("error").build()
                )
            }
                .status
                .code
        ).isEqualTo(Status.Code.INVALID_ARGUMENT)
    }

    @Test
    fun defaultErrorResponseIsUnknown(@Autowired channels: GrpcChannelFactory) {
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat(
            Assert.assertThrows(
                StatusRuntimeException::class.java
            ) { client.sayHello(HelloRequest.newBuilder().setName("internal").build()) }
                .status
                .code
        ).isEqualTo(Status.Code.UNKNOWN)
    }
}


@SpringBootTest
@AutoConfigureInProcessTransport
class ServerWithExceptionInInterceptorCall {
    @Test
    fun specificErrorResponse(@Autowired channels: GrpcChannelFactory) {
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat(
            Assert.assertThrows(
                StatusRuntimeException::class.java
            ) { client.sayHello(HelloRequest.newBuilder().setName("error").build()) }
                .status
                .code
        ).isEqualTo(Status.Code.INVALID_ARGUMENT)
    }

    @TestConfiguration
    internal open class TestConfig {
        @Bean
        @GlobalServerInterceptor
        open fun exceptionInterceptor(): ServerInterceptor {
            return CustomInterceptor()
        }

        internal class CustomInterceptor : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT?, RespT?>?, headers: Metadata?,
                next: ServerCallHandler<ReqT?, RespT?>?
            ): ServerCall.Listener<ReqT?>? {
                throw IllegalArgumentException("test")
            }
        }
    }
}


@SpringBootTest
@AutoConfigureInProcessTransport
class ServerWithExceptionInInterceptorListener {
    @Test
    fun specificErrorResponse(
        @Autowired channels: GrpcChannelFactory,
        @Autowired testConfig: TestConfig
    ) {
        testConfig.reset()
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat(
            Assert.assertThrows(
                StatusRuntimeException::class.java
            ) { client.sayHello(HelloRequest.newBuilder().setName("foo").build()) }
                .status
                .code
        ).isEqualTo(Status.Code.INVALID_ARGUMENT)
        Assertions.assertThat(TestConfig.readyCount.get()).isEqualTo(1)
        Assertions.assertThat(TestConfig.callCount.get()).isEqualTo(0)
        Assertions.assertThat(TestConfig.messageCount.get()).isEqualTo(0)
    }

    @TestConfiguration
    open class TestConfig {
        companion object {
            var callCount: AtomicInteger = AtomicInteger()
            var messageCount: AtomicInteger = AtomicInteger()
            var readyCount: AtomicInteger = AtomicInteger()
        }

        fun reset() {
            callCount.set(0)
            messageCount.set(0)
            readyCount.set(0)
        }


        @Bean
        @GlobalServerInterceptor
        open fun exceptionInterceptor(): ServerInterceptor {
            return CustomInterceptor()
        }

        internal class CustomInterceptor : ServerInterceptor {
            override fun <ReqT, RespT> interceptCall(
                call: ServerCall<ReqT?, RespT?>?, headers: Metadata?,
                next: ServerCallHandler<ReqT?, RespT?>
            ): ServerCall.Listener<ReqT?> {
                return CustomListener(next.startCall(call, headers))
            }
        }

        internal class CustomListener<ReqT>(private val delegate: ServerCall.Listener<ReqT?>?) :
            ForwardingServerCallListener<ReqT?>() {
            override fun onReady() {
                readyCount.incrementAndGet()
                throw IllegalArgumentException("test")
            }

            override fun onHalfClose() {
                callCount.incrementAndGet()
                super.onHalfClose()
            }

            override fun onMessage(message: ReqT?) {
                messageCount.incrementAndGet()
                super.onMessage(message)
            }

            override fun delegate(): ServerCall.Listener<ReqT?>? {
                return this.delegate
            }
        }
    }
}


@SpringBootTest("spring.grpc.server.exception-handler.enabled=false")
@AutoConfigureInProcessTransport
class ServerWithUnhandledException {
    @Test
    fun specificErrorResponse(@Autowired channels: GrpcChannelFactory) {
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat<Status.Code?>(
            Assert.assertThrows(StatusRuntimeException::class.java) {
                client.sayHello(
                    HelloRequest.newBuilder().setName("error").build()
                )
            }
                .status
                .code
        ).isEqualTo(Status.Code.UNKNOWN)
    }

    @Test
    fun defaultErrorResponseIsUnknown(@Autowired channels: GrpcChannelFactory) {
        val client = SimpleGrpc.newBlockingStub(channels.createChannel("0.0.0.0:0"))
        Assertions.assertThat(
            Assert.assertThrows(
                StatusRuntimeException::class.java
            ) { client.sayHello(HelloRequest.newBuilder().setName("internal").build()) }
                .status
                .code
        ).isEqualTo(Status.Code.UNKNOWN)
    }
}


@SpringBootTest(properties = ["spring.grpc.server.host=0.0.0.0", "spring.grpc.server.port=0"])
class ServerWithAnyIPv4AddressAndRandomPort {
    @Test
    fun servesResponseToClientWithAnyIPv4AddressAndRandomPort(
        @Autowired channels: GrpcChannelFactory,
        @LocalGrpcPort port: Int
    ) {
        assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port))
    }
}


@SpringBootTest(properties = ["spring.grpc.server.host=::", "spring.grpc.server.port=0"])
class ServerWithAnyIPv6AddressAndRandomPort {
    @Test
    fun servesResponseToClientWithAnyIPv4AddressAndRandomPort(
        @Autowired channels: GrpcChannelFactory,
        @LocalGrpcPort port: Int
    ) {
        assertThatResponseIsServedToChannel(channels.createChannel("0.0.0.0:" + port))
    }
}


@SpringBootTest(properties = ["spring.grpc.server.host=127.0.0.1", "spring.grpc.server.port=0"])
class ServerWithLocalhostAndRandomPort {
    @Test
    fun servesResponseToClientWithLocalhostAndRandomPort(
        @Autowired channels: GrpcChannelFactory,
        @LocalGrpcPort port: Int
    ) {
        assertThatResponseIsServedToChannel(channels.createChannel("127.0.0.1:" + port))
    }
}


@SpringBootTest(
    properties = ["spring.grpc.server.port=0", "spring.grpc.client.channels.test-channel.address=static://0.0.0.0:\${local.grpc.port}"]
)
@DirtiesContext
class ServerConfiguredWithStaticClientChannel {
    @Test
    fun servesResponseToClientWithConfiguredChannel(@Autowired channels: GrpcChannelFactory) {
        assertThatResponseIsServedToChannel(channels.createChannel("test-channel"))
    }
}


@SpringBootTest(properties = ["spring.grpc.server.address=unix:unix-test-channel"])
@EnabledOnOs(OS.LINUX)
class ServerWithUnixDomain {
    @Test
    fun clientChannelWithUnixDomain(@Autowired channels: GrpcChannelFactory) {
        assertThatResponseIsServedToChannel(
            channels.createChannel(
                "unix:unix-test-channel",
                ChannelBuilderOptions.defaults()
                    .withCustomizer<NettyChannelBuilder?>(GrpcChannelBuilderCustomizer { `__`: String?, b: NettyChannelBuilder? -> b!!.usePlaintext() })
            )
        )
    }
}


@SpringBootTest(
    properties = ["spring.grpc.server.port=0", "spring.grpc.client.channels.test-channel.address=static://0.0.0.0:\${local.grpc.port}", "spring.grpc.client.channels.test-channel.negotiation-type=TLS", "spring.grpc.client.channels.test-channel.secure=false"]
)
@ActiveProfiles("ssl")
@DirtiesContext
class ServerWithSsl {
    @Test
    fun clientChannelWithSsl(@Autowired channels: GrpcChannelFactory) {
        assertThatResponseIsServedToChannel(channels.createChannel("test-channel"))
    }
}


@SpringBootTest(
    properties = ["spring.grpc.server.port=0", "spring.grpc.server.ssl.client-auth=REQUIRE", "spring.grpc.server.ssl.secure=false", "spring.grpc.client.channels.test-channel.address=static://0.0.0.0:\${local.grpc.port}", "spring.grpc.client.channels.test-channel.ssl.bundle=ssltest", "spring.grpc.client.channels.test-channel.negotiation-type=TLS", "spring.grpc.client.channels.test-channel.secure=false"]
)
@ActiveProfiles("ssl")
@DirtiesContext
class ServerWithClientAuth {
    @Test
    fun clientChannelWithSsl(@Autowired channels: GrpcChannelFactory) {
        assertThatResponseIsServedToChannel(channels.createChannel("test-channel"))
    }
}

private fun assertThatResponseIsServedToChannel(clientChannel: ManagedChannel?) {
    val client = SimpleGrpc.newBlockingStub(clientChannel)
    val response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build())
    Assertions.assertThat(response.getMessage()).isEqualTo("Hello ==> Alien")
}

