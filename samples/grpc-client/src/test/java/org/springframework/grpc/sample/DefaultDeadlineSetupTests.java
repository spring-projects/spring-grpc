package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean;
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry;
import org.springframework.experimental.boot.test.context.DynamicProperty;
import org.springframework.experimental.boot.test.context.EnableDynamicProperty;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class DefaultDeadlineSetupTests {

	@Nested
	@SpringBootTest(properties = { "spring.grpc.client.default-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.default-channel.default-deadline=1s" })
	@DirtiesContext
	@EnabledIf("serverJarAvailable")
	class Deadline {

		static boolean serverJarAvailable() {
			return new File("../grpc-server/target/grpc-server-sample-0.11.0.jar").exists();
		}

		@Test
		void contextLoads() {
			// Real test case in ExtraConfiguration#runner(SimpleGrpc.SimpleBlockingStub)}
		}

		@TestConfiguration
		@EnableDynamicProperty
		static class ExtraConfiguration {

			@Bean
			@DynamicProperty(name = "local.grpc.port", value = "port")
			static CommonsExecWebServerFactoryBean grpcServer() {
				return CommonsExecWebServerFactoryBean.builder()
					.classpath(classpath -> classpath
						.entries(new MavenClasspathEntry("org.springframework.grpc:grpc-server-sample:0.11.0"))
						.files("target/test-classes"));
			}

			@Bean
			@Primary
			public CommandLineRunner otherRunner(SimpleGrpc.SimpleBlockingStub stub) {
				return args -> {
					var rs = stub.streamHello(HelloRequest.newBuilder().setName("Deadline").build());
					Assertions.assertNotNull(rs);
					StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
						while (rs.hasNext()) {
							System.out.println(rs.next());
						}
					});
					assertEquals(Status.Code.DEADLINE_EXCEEDED, exception.getStatus().getCode());
				};
			}

		}

	}

	@Nested
	@SpringBootTest(properties = { "spring.grpc.client.default-channel.address=static://0.0.0.0:${local.grpc.port}",
			"spring.grpc.client.default-channel.default-deadline=1s" })
	@DirtiesContext
	@EnabledIf("serverJarAvailable")
	class WithoutDeadline {

		static boolean serverJarAvailable() {
			return new File("../grpc-server/target/grpc-server-sample-0.11.0.jar").exists();
		}

		@Test
		void contextLoads() {
			// Real test case in ExtraConfiguration#runner(SimpleGrpc.SimpleBlockingStub)}
		}

		@TestConfiguration
		@EnableDynamicProperty
		static class ExtraConfiguration {

			@Bean
			@DynamicProperty(name = "local.grpc.port", value = "port")
			static CommonsExecWebServerFactoryBean grpcServer() {
				return CommonsExecWebServerFactoryBean.builder()
					.classpath(classpath -> classpath
						.entries(new MavenClasspathEntry("org.springframework.grpc:grpc-server-sample:0.11.0"))
						.files("target/test-classes"));
			}

			@Bean
			@Primary
			public CommandLineRunner otherRunner(SimpleGrpc.SimpleBlockingStub stub) {
				return args -> {
					var rs = stub.sayHello(HelloRequest.newBuilder().setName("WithoutDeadline").build());
					Assertions.assertNotNull(rs);
				};
			}

		}

	}

}