/*
 * Copyright 2024-present the original author or authors.
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

package org.springframework.grpc.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServerFactory}.
 */
class DefaultGrpcServerFactoryTests {

	@Nested
	class WithServiceFilter {

		@Test
		void whenNoFilterThenAllServicesAdded() {
			ServerServiceDefinition serviceDef1 = mock();
			ServerServiceDefinition serviceDef2 = mock();
			@SuppressWarnings({ "rawtypes", "unchecked" })
			DefaultGrpcServerFactory serverFactory = new DefaultGrpcServerFactory("myhost:5150", List.of(), null, null,
					null);
			serverFactory.addService(serviceDef2);
			serverFactory.addService(serviceDef1);
			assertThat(serverFactory)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.containsExactly(serviceDef2, serviceDef1);
		}

		@Test
		void whenFilterAllowsAllThenAllServicesAdded() {
			ServerServiceDefinition serviceDef1 = mock();
			ServerServiceDefinition serviceDef2 = mock();
			ServerServiceDefinitionFilter serviceFilter = (serviceDef, serviceFactory) -> true;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			DefaultGrpcServerFactory serverFactory = new DefaultGrpcServerFactory("myhost:5150", List.of(), null, null,
					null);
			serverFactory.setServiceFilter(serviceFilter);
			serverFactory.addService(serviceDef2);
			serverFactory.addService(serviceDef1);
			assertThat(serverFactory)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.containsExactly(serviceDef2, serviceDef1);
		}

		@Test
		void whenFilterAllowsOneThenOneServiceAdded() {
			ServerServiceDefinition serviceDef1 = mock();
			ServerServiceDefinition serviceDef2 = mock();
			ServerServiceDefinitionFilter serviceFilter = (serviceDef, serviceFactory) -> serviceDef == serviceDef1;
			@SuppressWarnings({ "rawtypes", "unchecked" })
			DefaultGrpcServerFactory serverFactory = new DefaultGrpcServerFactory("myhost:5150", List.of(), null, null,
					null);
			serverFactory.setServiceFilter(serviceFilter);
			serverFactory.addService(serviceDef2);
			serverFactory.addService(serviceDef1);
			assertThat(serverFactory)
				.extracting("serviceList", InstanceOfAssertFactories.list(ServerServiceDefinition.class))
				.containsExactly(serviceDef1);
		}

	}

	@Nested
	class AddressParser {

		@TestFactory
		List<DynamicTest> ipAddress() {
			return List.of(testIpAddress(":9999", new InetSocketAddress(9999)),
					testIpAddress("localhost:9999", new InetSocketAddress("localhost", 9999)),
					testIpAddress("localhost", new InetSocketAddress("localhost", 9090)),
					testIpAddress("*", new InetSocketAddress(9090)),
					testIpAddress("*:8888", new InetSocketAddress(8888)),
					testIpAddress("", new InetSocketAddress(9090)));
		}

		private DynamicTest testIpAddress(String address, SocketAddress expected) {
			return DynamicTest.dynamicTest("Socket address: " + address, () -> {
				var factory = new DefaultGrpcServerFactory<>(address, List.of());
				assertThat(factory.socketAddress()).isEqualTo(expected);
			});
		}

		@TestFactory
		List<DynamicTest> unsupportedAddress() {
			return List.of(testThrows("unix:dummy"), testThrows("in-process:"));
		}

		private DynamicTest testThrows(String address) {
			return DynamicTest.dynamicTest("Socket address: " + address,
					() -> assertThatExceptionOfType(UnsupportedOperationException.class)
						.isThrownBy(() -> new DefaultGrpcServerFactory<>(address, List.of()).socketAddress()));
		}

	}

}
