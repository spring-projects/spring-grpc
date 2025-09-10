package org.springframework.grpc.sample;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.experimental.boot.server.exec.CommonsExecWebServerFactoryBean;
import org.springframework.experimental.boot.server.exec.MavenClasspathEntry;
import org.springframework.experimental.boot.test.context.DynamicProperty;
import org.springframework.experimental.boot.test.context.EnableDynamicProperty;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(properties = "spring.grpc.client.default-channel.address=static://0.0.0.0:${local.grpc.port}")
@DirtiesContext
@EnabledIf("serverJarAvailable")
public class GrpcClientApplicationTests {

	public static void main(String[] args) {
		SpringApplication.from(GrpcClientApplication::main).with(ExtraConfiguration.class).run(args);
	}

	static boolean serverJarAvailable() {
		return new File("../grpc-server/target/grpc-server-sample-0.11.0.jar").exists();
	}

	@Test
	void contextLoads() {
	}

	@TestConfiguration(proxyBeanMethods = false)
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
		static BeanDefinitionRegistryPostProcessor startup(WebServer server) {
			return registry -> {
				server.getPort();
			};
		}

	}

}
