/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.sample.GrpcServerSideTests.TestConfig;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestPropertySource(properties = { "spring.grpc.client.channel.default.target=localhost:9090" })
@SpringJUnitConfig(TestConfig.class)
@AutoConfigureTestGrpcTransport
@ImportGrpcClients
public class GrpcServerSideTests {

	@Autowired
	private SimpleBlockingStub stub;

	@Test
	void contextLoads() {
		HelloRequest request = HelloRequest.newBuilder().setName("Test").build();
		HelloReply reply = this.stub.sayHello(request);
		assertThat(reply.getMessage()).contains(("Test"));
	}

	@TestConfiguration
	@Import({ GrpcServerService.class })
	@EnableAutoConfiguration
	static class TestConfig {

	}

}
