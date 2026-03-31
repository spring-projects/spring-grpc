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
import org.springframework.grpc.client.discovery.DiscoveryClientNameResolverProvider;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRefresher;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRegistrar;
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

}
