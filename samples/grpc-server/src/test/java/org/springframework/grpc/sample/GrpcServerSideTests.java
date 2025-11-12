/*
 * Copyright 2025-current the original author or authors.
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
package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureInProcessTransport;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.sample.GrpcServerSideTests.TestConfig;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc.SimpleBlockingStub;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestPropertySource(properties = { "spring.grpc.client.default-channel.address=localhost:9090" })
@SpringJUnitConfig(TestConfig.class)
@AutoConfigureInProcessTransport
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
