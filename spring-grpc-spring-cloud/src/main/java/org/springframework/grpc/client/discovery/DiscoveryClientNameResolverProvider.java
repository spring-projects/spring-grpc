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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

import org.jspecify.annotations.NullMarked;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.StringUtils;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;

/**
 * {@link NameResolverProvider} that resolves {@code discovery:///service-name} targets
 * through Spring Cloud {@link DiscoveryClient}.
 *
 * @author Hyacinth Contributor
 */
@NullMarked
public class DiscoveryClientNameResolverProvider extends NameResolverProvider {

	private final LogAccessor logger = new LogAccessor(getClass());

	private final DiscoveryClient discoveryClient;

	private final DiscoveryClientResolverProperties properties;

	private final Map<String, CopyOnWriteArrayList<DiscoveryClientNameResolver>> activeResolvers = new ConcurrentHashMap<>();

	public DiscoveryClientNameResolverProvider(DiscoveryClient discoveryClient,
			DiscoveryClientResolverProperties properties) {
		this.discoveryClient = discoveryClient;
		this.properties = properties;
	}

	@Override
	public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
		if (!GrpcDiscoveryConstants.DISCOVERY_SCHEME.equals(targetUri.getScheme())) {
			return null;
		}
		String serviceName = extractServiceName(targetUri);
		if (!StringUtils.hasText(serviceName)) {
			return null;
		}
		return new DiscoveryClientNameResolver(serviceName, args);
	}

	@Override
	public String getDefaultScheme() {
		return GrpcDiscoveryConstants.DISCOVERY_SCHEME;
	}

	@Override
	protected boolean isAvailable() {
		return true;
	}

	@Override
	protected int priority() {
		return 6;
	}

	public void refreshAll(String reason) {
		this.activeResolvers.values()
			.forEach((resolvers) -> resolvers.forEach((resolver) -> resolver.resolveNow(reason)));
	}

	private static String extractServiceName(URI targetUri) {
		String path = targetUri.getPath();
		if (StringUtils.hasText(path)) {
			return (path.startsWith("/")) ? path.substring(1) : path;
		}
		return targetUri.getAuthority();
	}

	private void register(String serviceName, DiscoveryClientNameResolver resolver) {
		this.activeResolvers.computeIfAbsent(serviceName, (key) -> new CopyOnWriteArrayList<>()).add(resolver);
	}

	private void unregister(String serviceName, DiscoveryClientNameResolver resolver) {
		CopyOnWriteArrayList<DiscoveryClientNameResolver> resolvers = this.activeResolvers.get(serviceName);
		if (resolvers == null) {
			return;
		}
		resolvers.remove(resolver);
		if (resolvers.isEmpty()) {
			this.activeResolvers.remove(serviceName, resolvers);
		}
	}

	private final class DiscoveryClientNameResolver extends NameResolver {

		private final String serviceName;

		private final Args args;

		private volatile Listener2 listener;

		private volatile ResolutionResult lastResolutionResult;

		private DiscoveryClientNameResolver(String serviceName, Args args) {
			this.serviceName = serviceName;
			this.args = args;
		}

		@Override
		public String getServiceAuthority() {
			return this.serviceName;
		}

		@Override
		public void start(Listener2 listener) {
			this.listener = listener;
			register(this.serviceName, this);
			resolveNow("startup");
		}

		@Override
		public void refresh() {
			resolveNow("grpc-refresh");
		}

		@Override
		public void shutdown() {
			unregister(this.serviceName, this);
		}

		private void resolveNow(String reason) {
			if (this.listener == null) {
				return;
			}
			Executor executor = this.args.getOffloadExecutor();
			Runnable resolveTask = () -> {
				try {
					List<ServiceInstance> instances = DiscoveryClientNameResolverProvider.this.discoveryClient
						.getInstances(this.serviceName);
					if (instances == null || instances.isEmpty()) {
						handleNoInstances(reason);
						return;
					}
					ResolutionResult.Builder resultBuilder = ResolutionResult.newBuilder()
						.setAddresses(instances.stream().map(this::toAddressGroup).toList());
					ConfigOrError serviceConfig = buildServiceConfig();
					if (serviceConfig != null) {
						resultBuilder.setServiceConfig(serviceConfig);
					}
					ResolutionResult result = resultBuilder.build();
					this.lastResolutionResult = result;
					this.args.getSynchronizationContext().execute(() -> this.listener.onResult(result));
				}
				catch (RuntimeException ex) {
					handleResolutionError(reason, ex);
				}
			};
			if (executor != null) {
				executor.execute(resolveTask);
				return;
			}
			resolveTask.run();
		}

		private void handleNoInstances(String reason) {
			if (this.lastResolutionResult != null) {
				DiscoveryClientNameResolverProvider.this.logger
					.warn(() -> "No instances found for discovery target, keeping previous resolution. serviceName="
							+ this.serviceName + ", reason=" + reason);
				return;
			}
			DiscoveryClientNameResolverProvider.this.logger
				.warn(() -> "No instances found for discovery target. serviceName=" + this.serviceName + ", reason="
						+ reason);
			Status status = Status.UNAVAILABLE.withDescription("No instance for " + this.serviceName);
			this.args.getSynchronizationContext().execute(() -> this.listener.onError(status));
		}

		private void handleResolutionError(String reason, RuntimeException ex) {
			DiscoveryClientNameResolverProvider.this.logger.error(ex,
					() -> "Failed to resolve discovery target. serviceName=" + this.serviceName + ", reason=" + reason);
			Status status = Status.UNAVAILABLE
				.withDescription("Failed to resolve discovery target '" + this.serviceName + "'")
				.withCause(ex);
			this.args.getSynchronizationContext().execute(() -> this.listener.onError(status));
		}

		private ConfigOrError buildServiceConfig() {
			String policy = DiscoveryClientNameResolverProvider.this.properties.getLoadBalancingPolicy();
			if (!StringUtils.hasText(policy)) {
				return null;
			}
			Map<String, ?> serviceConfig = Map.of("loadBalancingConfig",
					List.of(Map.of(policy, Collections.emptyMap())));
			return this.args.getServiceConfigParser().parseServiceConfig(serviceConfig);
		}

		private EquivalentAddressGroup toAddressGroup(ServiceInstance instance) {
			int grpcPort = resolveGrpcPort(instance);
			String host = instance.getHost();
			DiscoveryClientNameResolverProvider.this.logger
				.debug(() -> "Discovery resolver selected instance. serviceName=" + this.serviceName + ", host=" + host
						+ ", grpcPort=" + grpcPort + ", metadata=" + instance.getMetadata());
			return new EquivalentAddressGroup(new InetSocketAddress(host, grpcPort));
		}

		private int resolveGrpcPort(ServiceInstance instance) {
			String grpcPort = instance.getMetadata().get(GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY);
			if (!StringUtils.hasText(grpcPort)) {
				return instance.getPort();
			}
			try {
				return Integer.parseInt(grpcPort);
			}
			catch (NumberFormatException ex) {
				DiscoveryClientNameResolverProvider.this.logger
					.warn(() -> "Invalid metadata.gRPC_port detected, fallback to instance port. serviceName="
							+ this.serviceName + ", host=" + instance.getHost() + ", value=" + grpcPort);
				return instance.getPort();
			}
		}

	}

}
