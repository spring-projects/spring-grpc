/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.grpc.client.discovery.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.discovery.DiscoveryClientNameResolverProvider;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRefresher;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRegistrar;
import org.springframework.grpc.client.discovery.GrpcDiscoveryConstants;
import org.springframework.grpc.client.discovery.GrpcPortMetadataRegistrationBeanPostProcessor;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * Tests for {@link DiscoveryClientNameResolverAutoConfiguration}.
 */
class DiscoveryClientNameResolverAutoConfigurationTests {

	@Test
	void autoConfigurationBacksOffWithoutDiscoveryClient() {
		try (var context = new AnnotationConfigApplicationContext()) {
			context.register(DiscoveryClientNameResolverAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeansOfType(DiscoveryClientNameResolverProvider.class)).isEmpty();
			assertThat(context.getBeansOfType(DiscoveryNameResolverRegistrar.class)).isEmpty();
			assertThat(context.getBeansOfType(DiscoveryNameResolverRefresher.class)).isEmpty();
		}
	}

	@Test
	void autoConfigurationRegistersDiscoveryBeansWhenEnabled() {
		try (var context = new AnnotationConfigApplicationContext()) {
			context.registerBean(DiscoveryClient.class, () -> mock(DiscoveryClient.class));
			context.register(DiscoveryClientNameResolverAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeansOfType(DiscoveryClientNameResolverProvider.class)).hasSize(1);
			assertThat(context.getBeansOfType(DiscoveryNameResolverRegistrar.class)).hasSize(1);
			assertThat(context.getBeansOfType(DiscoveryNameResolverRefresher.class)).hasSize(1);
		}
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		try (var context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
					"spring.grpc.client.discovery.enabled=false");
			context.registerBean(DiscoveryClient.class, () -> mock(DiscoveryClient.class));
			context.register(DiscoveryClientNameResolverAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeansOfType(DiscoveryClientNameResolverProvider.class)).isEmpty();
		}
	}

	@Test
	void channelFactoryDependsOnDiscoveryRegistrar() {
		try (var context = new AnnotationConfigApplicationContext()) {
			context.registerBean(DiscoveryClient.class, () -> mock(DiscoveryClient.class));
			context.registerBean("grpcChannelFactory", GrpcChannelFactory.class, () -> mock(GrpcChannelFactory.class));
			context.register(DiscoveryClientNameResolverAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeanFactory().getBeanDefinition("grpcChannelFactory").getDependsOn())
				.contains("discoveryNameResolverRegistrar");
		}
	}

	@Test
	void serviceRegistrationMetadataIsPublishedWhenEnabled() {
		try (var context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "spring.grpc.server.port=9091");
			context.register(TestRegistrationConfiguration.class, GrpcDiscoveryServiceRegistryAutoConfiguration.class);
			context.refresh();
			TestRegistration registration = context.getBean(TestRegistration.class);
			assertThat(registration.getMetadata()).containsEntry(GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY, "9091");
			assertThat(context.getBeansOfType(GrpcPortMetadataRegistrationBeanPostProcessor.class)).hasSize(1);
		}
	}

	@Test
	void serviceRegistrationMetadataPublicationCanBeDisabled() {
		try (var context = new AnnotationConfigApplicationContext()) {
			TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context, "spring.grpc.server.port=9091",
					"spring.grpc.server.discovery.publish-metadata=false");
			context.register(TestRegistrationConfiguration.class, GrpcDiscoveryServiceRegistryAutoConfiguration.class);
			context.refresh();
			TestRegistration registration = context.getBean(TestRegistration.class);
			assertThat(registration.getMetadata()).doesNotContainKey(GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY);
			assertThat(context.getBeansOfType(GrpcPortMetadataRegistrationBeanPostProcessor.class)).isEmpty();
		}
	}

	static class TestRegistrationConfiguration {

		@org.springframework.context.annotation.Bean
		TestRegistration registration() {
			return new TestRegistration();
		}

	}

	static class TestRegistration implements org.springframework.cloud.client.serviceregistry.Registration {

		private final java.util.Map<String, String> metadata = new java.util.LinkedHashMap<>();

		@Override
		public String getServiceId() {
			return "test-service";
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return 8080;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public java.net.URI getUri() {
			return java.net.URI.create("http://127.0.0.1:8080");
		}

		@Override
		public java.util.Map<String, String> getMetadata() {
			return this.metadata;
		}

	}

}
