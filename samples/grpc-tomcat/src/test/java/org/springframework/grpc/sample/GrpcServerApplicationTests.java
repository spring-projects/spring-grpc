package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.grpc.client.channel.default.target=0.0.0.0:${local.server.port}", "stream.count=2" })
@DirtiesContext
@Disabled("Need to migrate to Spring Boot 4.1.x")
public class GrpcServerApplicationTests {

	private static Log log = LogFactory.getLog(GrpcServerApplicationTests.class);

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class).run(args);
	}

	@Autowired
	private SimpleGrpc.SimpleBlockingStub stub;

	@Test
	void contextLoads() {
	}

	@Test
	void serverResponds() {
		log.info("Testing");
		HelloReply response = stub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@Test
	void streamResponds() {
		Iterator<HelloReply> response = stub.streamHello(HelloRequest.newBuilder().setName("Alien").build());
		assertEquals("Hello(0) ==> Alien", response.next().getMessage());
		while (response.hasNext()) {
			log.info(response.next().getMessage());
		}
	}

}
