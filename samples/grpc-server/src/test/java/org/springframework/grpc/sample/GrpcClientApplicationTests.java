package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.client.FutureStubFactory;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.test.AutoConfigureInProcessTransport;

import io.grpc.stub.AbstractStub;

public class GrpcClientApplicationTests {

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class NoAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void noStubIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).isEmpty();
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class DefaultAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void onlyDefaultStubIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleBlockingStub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(1);
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class SpecificAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void stubOfCorrectTypeIsCreated() {
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleFutureStub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isTrue();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(2);
		}

		@TestConfiguration
		@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = FutureStubFactory.class)
		@ImportGrpcClients(basePackageClasses = SimpleGrpc.class)
		static class TestConfig {

		}

	}

}
