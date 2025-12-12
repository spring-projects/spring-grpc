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
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.grpc.internal.GrpcUtils;

import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServerFactory}.
 */
class DefaultGrpcServerFactoryTests {

	@Nested
	class GetPortAndHostname {

		@Test
		void portDelegatesToGrpcUtils() {
			var serverFactory = new DefaultGrpcServerFactory<>("*:9090", Collections.emptyList(), null, null, null);
			try (MockedStatic<GrpcUtils> mockedStaticGrpcUtils = Mockito.mockStatic(GrpcUtils.class)) {
				serverFactory.port();
				mockedStaticGrpcUtils.verify(() -> GrpcUtils.getPort("*:9090"));
			}
		}

		@Test
		void hostnameDelegatesToGrpcUtils() {
			var serverFactory = new DefaultGrpcServerFactory<>("*:9090", Collections.emptyList(), null, null, null);
			try (MockedStatic<GrpcUtils> mockedStaticGrpcUtils = Mockito.mockStatic(GrpcUtils.class)) {
				serverFactory.hostname();
				mockedStaticGrpcUtils.verify(() -> GrpcUtils.getHostName("*:9090"));
			}
		}

	}

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

}
