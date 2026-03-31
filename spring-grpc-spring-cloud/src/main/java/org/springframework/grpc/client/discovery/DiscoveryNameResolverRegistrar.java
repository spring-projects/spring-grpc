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

import org.jspecify.annotations.NullMarked;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.log.LogAccessor;

import io.grpc.NameResolverRegistry;

/**
 * Registers a discovery-backed {@link io.grpc.NameResolverProvider} with the gRPC
 * registry.
 *
 * @author Hyacinth Contributor
 */
@NullMarked
public class DiscoveryNameResolverRegistrar implements InitializingBean, DisposableBean {

	private final LogAccessor logger = new LogAccessor(getClass());

	private final DiscoveryClientNameResolverProvider provider;

	public DiscoveryNameResolverRegistrar(DiscoveryClientNameResolverProvider provider) {
		this.provider = provider;
	}

	@Override
	public void afterPropertiesSet() {
		NameResolverRegistry.getDefaultRegistry().register(this.provider);
		this.logger.info(() -> "Registered discovery NameResolverProvider for scheme '"
				+ GrpcDiscoveryConstants.DISCOVERY_SCHEME + "'");
	}

	@Override
	public void destroy() {
		NameResolverRegistry.getDefaultRegistry().deregister(this.provider);
	}

}
