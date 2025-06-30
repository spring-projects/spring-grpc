/*
 * Copyright 2023-2024 the original author or authors.
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

package org.springframework.grpc.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.grpc.server.service.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsServiceConfig.SERVICE_A;
import static org.springframework.grpc.server.service.DefaultGrpcServiceDiscovererTests.DefaultGrpcServiceDiscovererTestsServiceConfig.SERVICE_B;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;

/**
 * Tests for {@link DefaultGrpcServiceDiscoverer}.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceDiscovererTests {

	@Test
	void whenNoServicesRegisteredThenListServiceNamesReturnsEmptyList() {
		new ApplicationContextRunner().withUserConfiguration(DefaultGrpcServiceDiscovererTestsBaseConfig.class)
			.run((context) -> assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
				.extracting(DefaultGrpcServiceDiscoverer::listServiceNames, InstanceOfAssertFactories.LIST)
				.isEmpty());
	}

	@Test
	void whenServicesRegisteredThenListServiceNamesReturnsNames() {
		new ApplicationContextRunner()
			.withUserConfiguration(DefaultGrpcServiceDiscovererTestsBaseConfig.class,
					DefaultGrpcServiceDiscovererTestsServiceConfig.class)
			.run((context) -> assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
				.extracting(DefaultGrpcServiceDiscoverer::listServiceNames, InstanceOfAssertFactories.LIST)
				.containsExactly("serviceB", "serviceA"));

	}

	@Test
	void servicesAreFoundInProperOrderWithExpectedGrpcServiceAnnotations() {
		new ApplicationContextRunner()
			.withUserConfiguration(DefaultGrpcServiceDiscovererTestsBaseConfig.class,
					DefaultGrpcServiceDiscovererTestsServiceConfig.class)
			.run((context) -> assertThat(context).getBean(DefaultGrpcServiceDiscoverer.class)
				.extracting(DefaultGrpcServiceDiscoverer::findServices,
						InstanceOfAssertFactories.list(ServerServiceDefinitionSpec.class))
				.satisfies((serviceSpecs) -> {
					assertThat(serviceSpecs).hasSize(2);
					assertThat(serviceSpecs).element(0).isEqualTo(new ServerServiceDefinitionSpec(SERVICE_B, null));
					assertThat(serviceSpecs).element(1).satisfies((spec) -> {
						assertThat(spec.service()).isEqualTo(SERVICE_A);
						assertThat(spec.serviceInfo()).isNotNull();
					});
				}));
	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultGrpcServiceDiscovererTestsBaseConfig {

		@Bean
		GrpcServiceDiscoverer grpcServiceDiscoverer(ApplicationContext applicationContext) {
			return new DefaultGrpcServiceDiscoverer(applicationContext);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DefaultGrpcServiceDiscovererTestsServiceConfig {

		static BindableService SERVICE_A = Mockito.mock();

		static ServerServiceDefinition SERVICE_DEF_A = Mockito.mock();

		static BindableService SERVICE_B = Mockito.mock();

		static ServerServiceDefinition SERVICE_DEF_B = Mockito.mock();

		@GrpcService
		@Bean
		@Order(200)
		BindableService serviceA() {
			ServiceDescriptor descriptor = mock();
			when(descriptor.getName()).thenReturn("serviceA");
			when(SERVICE_DEF_A.getServiceDescriptor()).thenReturn(descriptor);
			when(SERVICE_A.bindService()).thenReturn(SERVICE_DEF_A);
			return SERVICE_A;
		}

		@Bean
		@Order(100)
		BindableService serviceB() {
			ServiceDescriptor descriptor = mock();
			when(descriptor.getName()).thenReturn("serviceB");
			when(SERVICE_DEF_B.getServiceDescriptor()).thenReturn(descriptor);
			when(SERVICE_B.bindService()).thenReturn(SERVICE_DEF_B);
			return SERVICE_B;
		}

	}

}
