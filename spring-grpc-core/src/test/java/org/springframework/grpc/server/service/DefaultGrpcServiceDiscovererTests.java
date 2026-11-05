/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
		var context = new AnnotationConfigApplicationContext(DefaultGrpcServiceDiscovererTestsBaseConfig.class);
		assertThat(context.getBean(DefaultGrpcServiceDiscoverer.class))
			.extracting(DefaultGrpcServiceDiscoverer::listServiceNames, InstanceOfAssertFactories.LIST)
			.isEmpty();
	}

	@Test
	void whenServicesRegisteredThenListServiceNamesReturnsNames() {
		var context = new AnnotationConfigApplicationContext(DefaultGrpcServiceDiscovererTestsBaseConfig.class,
				DefaultGrpcServiceDiscovererTestsServiceConfig.class);
		assertThat(context.getBean(DefaultGrpcServiceDiscoverer.class))
			.extracting(DefaultGrpcServiceDiscoverer::listServiceNames, InstanceOfAssertFactories.LIST)
			.containsExactly("serviceB", "serviceA");

	}

	@Test
	void servicesAreFoundInProperOrderWithExpectedGrpcServiceAnnotations() {
		var context = new AnnotationConfigApplicationContext(DefaultGrpcServiceDiscovererTestsBaseConfig.class,
				DefaultGrpcServiceDiscovererTestsServiceConfig.class);
		assertThat(context.getBean(DefaultGrpcServiceDiscoverer.class))
			.extracting(DefaultGrpcServiceDiscoverer::findServices,
					InstanceOfAssertFactories.list(GrpcServiceSpec.class))
			.satisfies((serviceSpecs) -> {
				assertThat(serviceSpecs).hasSize(2);
				assertThat(serviceSpecs).element(0).isEqualTo(new GrpcServiceSpec(SERVICE_B, null));
				assertThat(serviceSpecs).element(1).satisfies((spec) -> {
					assertThat(spec.service()).isEqualTo(SERVICE_A);
					assertThat(spec.serviceInfo()).isNotNull();
				});
			});
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
