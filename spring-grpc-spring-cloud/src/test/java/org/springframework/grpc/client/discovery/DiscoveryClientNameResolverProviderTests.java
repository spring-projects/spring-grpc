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

package org.springframework.grpc.client.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.Status;
import io.grpc.SynchronizationContext;

/**
 * Tests for {@link DiscoveryClientNameResolverProvider}.
 */
class DiscoveryClientNameResolverProviderTests {

	@Test
	void resolvesDiscoveryTargetAndAppliesConfiguredLoadBalancingPolicy() throws Exception {
		DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
		DiscoveryClientResolverProperties properties = new DiscoveryClientResolverProperties();
		when(discoveryClient.getInstances("orders-service"))
			.thenReturn(List.of(instance("orders-service", "127.0.0.1", 8080, Map.of("gRPC_port", "9090")),
					instance("orders-service", "127.0.0.2", 8081, Map.of())));
		var provider = new DiscoveryClientNameResolverProvider(discoveryClient, properties);
		var listener = mock(NameResolver.Listener2.class);
		var resolver = provider.newNameResolver(new URI("discovery:///orders-service"), args());
		assertThat(resolver).isNotNull();
		resolver.start(listener);
		verify(listener).onResult(assertArg((result) -> {
			assertThat(result.getAddresses()).hasSize(2);
			assertThat(result.getAddresses()).extracting(EquivalentAddressGroup::getAddresses)
				.allSatisfy((addresses) -> {
					assertThat(addresses).hasSize(1);
				});
			assertThat(result.getAddresses().get(0).getAddresses().get(0)).hasToString("/127.0.0.1:9090");
			assertThat(result.getAddresses().get(1).getAddresses().get(0)).hasToString("/127.0.0.2:8081");
			assertThat(result.getServiceConfig()).isNotNull();
			assertThat(result.getServiceConfig().getConfig())
				.isEqualTo(Map.of("loadBalancingConfig", List.of(Map.of("round_robin", Map.of()))));
		}));
	}

	@Test
	void keepsPreviousResolutionWhenRefreshTemporarilyReturnsNoInstances() throws Exception {
		DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
		DiscoveryClientResolverProperties properties = new DiscoveryClientResolverProperties();
		when(discoveryClient.getInstances("orders-service"))
			.thenReturn(List.of(instance("orders-service", "127.0.0.1", 8080, Map.of("gRPC_port", "9090"))))
			.thenReturn(List.of());
		var provider = new DiscoveryClientNameResolverProvider(discoveryClient, properties);
		var listener = mock(NameResolver.Listener2.class);
		var resolver = provider.newNameResolver(new URI("discovery:///orders-service"), args());
		assertThat(resolver).isNotNull();
		resolver.start(listener);
		resolver.refresh();
		verify(listener, Mockito.times(1)).onResult(Mockito.any());
		Mockito.verify(listener, Mockito.never()).onError(Mockito.any());
	}

	@Test
	void reportsUnavailableWhenNoInstancesExistOnInitialResolution() throws Exception {
		DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
		when(discoveryClient.getInstances("orders-service")).thenReturn(List.of());
		var provider = new DiscoveryClientNameResolverProvider(discoveryClient,
				new DiscoveryClientResolverProperties());
		var listener = mock(NameResolver.Listener2.class);
		var resolver = provider.newNameResolver(new URI("discovery:///orders-service"), args());
		assertThat(resolver).isNotNull();
		resolver.start(listener);
		verify(listener).onError(assertArg((status) -> {
			assertThat(status.getCode()).isEqualTo(Status.Code.UNAVAILABLE);
			assertThat(status.getDescription()).isEqualTo("No instance for orders-service");
		}));
	}

	@Test
	void nonDiscoverySchemeReturnsNull() throws Exception {
		var provider = new DiscoveryClientNameResolverProvider(mock(DiscoveryClient.class),
				new DiscoveryClientResolverProperties());
		assertThat(provider.newNameResolver(new URI("dns:///orders-service"), args())).isNull();
	}

	private static DefaultServiceInstance instance(String serviceId, String host, int port,
			Map<String, String> metadata) {
		DefaultServiceInstance instance = new DefaultServiceInstance(serviceId + "-" + host + "-" + port, serviceId,
				host, port, false, metadata);
		return instance;
	}

	private static NameResolver.Args args() {
		return NameResolver.Args.newBuilder()
			.setDefaultPort(443)
			.setProxyDetector((targetAddress) -> null)
			.setSynchronizationContext(new SynchronizationContext((t, e) -> {
			}))
			.setServiceConfigParser(new NameResolver.ServiceConfigParser() {
				@Override
				public NameResolver.ConfigOrError parseServiceConfig(Map<String, ?> serviceConfig) {
					return NameResolver.ConfigOrError.fromConfig(serviceConfig);
				}
			})
			.setOffloadExecutor(Runnable::run)
			.build();
	}

}
