package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.interceptor.security.BasicAuthenticationInterceptor;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.test.annotation.DirtiesContext;

import io.grpc.Status.Code;
import io.grpc.StatusRuntimeException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "spring.grpc.client.channel.default.target=0.0.0.0:${local.server.port}")
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run(args);
	}

	@Autowired
	@Qualifier("simpleBlockingStub")
	private SimpleGrpc.SimpleBlockingStub stub;

	@Autowired
	@Qualifier("basic")
	private SimpleGrpc.SimpleBlockingStub basic;

	@Test
	@DirtiesContext
	void contextLoads() {
	}

	@Test
	@DirtiesContext
	void unauthenticated() {
		StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
				() -> stub.sayHello(HelloRequest.newBuilder().setName("Alien").build()));
		assertEquals(Code.UNAUTHENTICATED, exception.getStatus().getCode());
	}

	@Test
	@DirtiesContext
	void authenticated() {
		log.info("Testing");
		HelloReply response = basic.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@TestConfiguration
	@ImportGrpcClients(basePackageClasses = GrpcServerApplication.class)
	static class ExtraConfiguration {

		@Bean
		@Lazy
		SimpleGrpc.SimpleBlockingStub basic(GrpcChannelFactory channels, @LocalServerPort int port) {
			return SimpleGrpc.newBlockingStub(channels.createChannel("default", ChannelBuilderOptions.defaults()
				.withInterceptors(List.of(new BasicAuthenticationInterceptor("user", "user")))));
		}

	}

}
