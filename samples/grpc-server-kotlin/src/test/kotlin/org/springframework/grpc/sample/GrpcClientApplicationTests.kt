package org.springframework.grpc.sample

import io.grpc.stub.AbstractStub
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.grpc.client.FutureStubFactory
import org.springframework.grpc.client.ImportGrpcClients
import org.springframework.grpc.sample.proto.SimpleGrpc
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleFutureStub
import org.springframework.grpc.test.AutoConfigureInProcessTransport


@SpringBootTest
@AutoConfigureInProcessTransport
class NoAutowiredClients {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun noStubIsCreated() {
        assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse()
        assertThat(context.containsBeanDefinition("simpleStub")).isFalse()
        assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse()
        assertThat(context.getBeanNamesForType(AbstractStub::class.java))
            .isEmpty()
    }
}

@SpringBootTest(properties = ["spring.grpc.client.default-channel.address=0.0.0.0:9090"])
@AutoConfigureInProcessTransport
class DefaultAutowiredClients {
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun onlyDefaultStubIsCreated() {
        Assertions.assertThat(context.containsBeanDefinition("simpleBlockingStub")).isTrue()
        assertThat(
            context.getBean(
                SimpleBlockingStub::class.java
            )
        ).isNotNull()
        assertThat(context.containsBeanDefinition("simpleStub")).isFalse()
        assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse()
        assertThat(context.getBeanNamesForType(AbstractStub::class.java))
            .hasSize(1)
    }
}

@SpringBootTest(
    properties = ["spring.grpc.client.default-channel.address=0.0.0.0:9090"]
)
@AutoConfigureInProcessTransport
class SpecificAutowiredClients {
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun stubOfCorrectTypeIsCreated() {
        assertThat(context.containsBeanDefinition("simpleFutureStub")).isTrue()
        assertThat(
            context.getBean(
                SimpleFutureStub::class.java
            )
        ).isNotNull()
        assertThat(context.containsBeanDefinition("simpleStub")).isFalse()
        assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse()
        assertThat(context.getBeanNamesForType(AbstractStub::class.java))
            .hasSize(1)
    }

    @TestConfiguration
    @ImportGrpcClients(basePackageClasses = [SimpleGrpc::class], factory = FutureStubFactory::class)
    internal open class TestConfig
}

