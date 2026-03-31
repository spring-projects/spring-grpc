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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.discovery.DiscoveryClientNameResolverProvider;
import org.springframework.grpc.client.discovery.DiscoveryClientResolverProperties;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRefresher;
import org.springframework.grpc.client.discovery.DiscoveryNameResolverRegistrar;

import io.grpc.NameResolver;

/**
 * Auto-configuration for Spring Cloud backed gRPC name resolution.
 *
 * @author Hyacinth Contributor
 */
@AutoConfiguration
@ConditionalOnClass({ NameResolver.class, DiscoveryClient.class })
@ConditionalOnBean(DiscoveryClient.class)
@ConditionalOnProperty(prefix = "spring.grpc.client.discovery", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(DiscoveryClientResolverProperties.class)
public class DiscoveryClientNameResolverAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	DiscoveryClientNameResolverProvider discoveryClientNameResolverProvider(DiscoveryClient discoveryClient,
			DiscoveryClientResolverProperties properties) {
		return new DiscoveryClientNameResolverProvider(discoveryClient, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	DiscoveryNameResolverRegistrar discoveryNameResolverRegistrar(DiscoveryClientNameResolverProvider provider) {
		return new DiscoveryNameResolverRegistrar(provider);
	}

	@Bean
	@ConditionalOnMissingBean
	DiscoveryNameResolverRefresher discoveryNameResolverRefresher(DiscoveryClientNameResolverProvider provider) {
		return new DiscoveryNameResolverRefresher(provider);
	}

}
