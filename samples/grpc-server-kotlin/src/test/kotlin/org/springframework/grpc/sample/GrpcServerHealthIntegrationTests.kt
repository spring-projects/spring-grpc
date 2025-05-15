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

import io.grpc.StatusRuntimeException
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.health.v1.HealthGrpc
import io.grpc.health.v1.HealthGrpc.HealthBlockingStub
import io.grpc.protobuf.services.HealthStatusManager
import org.assertj.core.api.Assertions
import org.assertj.core.api.ThrowableAssert
import org.awaitility.Awaitility
import org.awaitility.core.ThrowingRunnable
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.grpc.client.GrpcChannelFactory
import org.springframework.grpc.sample.proto.HelloRequest
import org.springframework.grpc.sample.proto.SimpleGrpc
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub
import org.springframework.grpc.test.AutoConfigureInProcessTransport
import org.springframework.test.annotation.DirtiesContext
import java.time.Duration

/**
 * Integration tests for gRPC server health feature.
 */

@SpringBootTest(
    properties = ["spring.grpc.server.port=0", "spring.grpc.client.channels.health-test.address=static://0.0.0.0:\${local.grpc.port}", "spring.grpc.client.channels.health-test.health.enabled=true", "spring.grpc.client.channels.health-test.health.service-name=my-service"]
)
@DirtiesContext
class WithClientHealthEnabled {
    @Test
    fun loadBalancerRespectsServerHealth(
        @Autowired channels: GrpcChannelFactory,
        @Autowired healthStatusManager: HealthStatusManager
    ) {
        val channel = channels.createChannel("health-test")
        val client = SimpleGrpc.newBlockingStub(channel)

        // put the service up (SERVING) and give load balancer time to update
        updateHealthStatusAndWait("my-service", ServingStatus.SERVING, healthStatusManager)

        // initially the status should be SERVING
        assertThatResponseIsServedToChannel(client)

        // put the service down (NOT_SERVING) and give load balancer time to update
        updateHealthStatusAndWait("my-service", ServingStatus.NOT_SERVING, healthStatusManager)

        // now the request should fail
        assertThatResponseIsNotServedToChannel(client)

        // put the service up (SERVING) and give load balancer time to update
        updateHealthStatusAndWait("my-service", ServingStatus.SERVING, healthStatusManager)

        // now the request should pass
        assertThatResponseIsServedToChannel(client)
    }

    private fun updateHealthStatusAndWait(
        serviceName: String?, healthStatus: ServingStatus,
        healthStatusManager: HealthStatusManager
    ) {
        healthStatusManager.setStatus(serviceName, healthStatus)
        try {
            Thread.sleep(2000L)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    private fun assertThatResponseIsServedToChannel(client: SimpleBlockingStub) {
        val response = client.sayHello(HelloRequest.newBuilder().setName("Alien").build())
        Assertions.assertThat(response.getMessage()).isEqualTo("Hello ==> Alien")
    }

    private fun assertThatResponseIsNotServedToChannel(client: SimpleBlockingStub) {
        Assertions.assertThatExceptionOfType<StatusRuntimeException?>(StatusRuntimeException::class.java)
            .isThrownBy(ThrowableAssert.ThrowingCallable {
                client.sayHello(
                    HelloRequest.newBuilder().setName("Alien").build()
                )
            })
            .withMessageContaining("UNAVAILABLE: Health-check service responded NOT_SERVING for 'my-service'")
    }
}


@SpringBootTest(
    properties = ["spring.grpc.server.health.actuator.health-indicator-paths=custom", "spring.grpc.server.health.actuator.update-initial-delay=3s", "spring.grpc.server.health.actuator.update-rate=3s", "management.health.defaults.enabled=true"]
)
@AutoConfigureInProcessTransport
@DirtiesContext
class WithActuatorHealthAdapter {
    @Test
    fun healthIndicatorsAdaptedToGrpcHealthStatus(
        @Autowired channels: GrpcChannelFactory,
        @Autowired customHealthIndicator: CustomHealthIndicator
    ) {
        val channel = channels.createChannel("0.0.0.0:0")
        val healthStub = HealthGrpc.newBlockingStub(channel)
        val serviceName = "custom"

        // initially the status should be SERVING
        assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4))

        // put the service down and the status should then be NOT_SERVING
        customHealthIndicator.SERVICE_IS_UP = false
        assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.NOT_SERVING, Duration.ofSeconds(4))

        // put the service up and the status should be SERVING
        customHealthIndicator.SERVICE_IS_UP = true
        assertThatGrpcHealthStatusIs(healthStub, serviceName, ServingStatus.SERVING, Duration.ofSeconds(4))
    }

    private fun assertThatGrpcHealthStatusIs(
        healthBlockingStub: HealthBlockingStub, service: String,
        expectedStatus: ServingStatus?, maxWaitTime: Duration?
    ) {
        Awaitility.await().atMost(maxWaitTime).ignoreException(StatusRuntimeException::class.java).untilAsserted {
            val healthRequest = HealthCheckRequest.newBuilder().setService(service).build()
            val healthResponse = healthBlockingStub.check(healthRequest)
            Assertions.assertThat(healthResponse.getStatus()).isEqualTo(expectedStatus)
            // verify the overall status as well
            val overallHealthRequest = HealthCheckRequest.newBuilder().setService("").build()
            val overallHealthResponse = healthBlockingStub.check(overallHealthRequest)
            Assertions.assertThat(overallHealthResponse.getStatus()).isEqualTo(expectedStatus)
        }
    }

    @TestConfiguration
    internal open class MyHealthIndicatorsConfig {
        @ConditionalOnEnabledHealthIndicator("custom")
        @Bean
        open fun customHealthIndicator(): CustomHealthIndicator {
            return CustomHealthIndicator()
        }
    }

    class CustomHealthIndicator : HealthIndicator {
        override fun health(): Health? {
            return if (SERVICE_IS_UP) Health.up().build() else Health.down().build()
        }

        var SERVICE_IS_UP: Boolean = true
    }
}

