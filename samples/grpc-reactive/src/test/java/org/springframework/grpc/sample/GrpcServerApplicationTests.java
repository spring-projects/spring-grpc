package org.springframework.grpc.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.ReactorSimpleGrpc;
import org.springframework.grpc.sample.proto.ReactorSimpleGrpc.ReactorSimpleStub;
import org.springframework.test.annotation.DirtiesContext;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@DirtiesContext
@AutoConfigureInProcessTransport
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class, ExtraConfiguration.class).run();
	}

	@Autowired
	private ReactorSimpleStub stub;

	@Test
	void contextLoads() {
	}

	@Test
	void serverResponds() {
		log.info("Testing");
		Mono<HelloReply> reply = stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		StepVerifier.create(reply.map(response -> response.getMessage()))
			.expectNext("Hello ==> Alien")
			.verifyComplete();
	}

	@TestConfiguration
	@ImportGrpcClients(types = ReactorSimpleGrpc.ReactorSimpleStub.class)
	static class ExtraConfiguration {

	}

}
