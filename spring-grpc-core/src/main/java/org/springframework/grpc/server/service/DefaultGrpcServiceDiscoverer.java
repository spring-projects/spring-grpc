/*
 * Copyright 2023-2025 the original author or authors.
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

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServiceDescriptor;

/**
 * Default {@link GrpcServiceDiscoverer} implementation that finds all
 * {@link BindableService} beans in the application context.
 *
 * @author Chris Bono
 */
public class DefaultGrpcServiceDiscoverer implements GrpcServiceDiscoverer {

	private final ApplicationContext applicationContext;

	public DefaultGrpcServiceDiscoverer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public List<ServerServiceDefinitionSpec> findServices() {
		return ApplicationContextBeanLookupUtils
			.getOrderedBeansWithAnnotation(this.applicationContext, BindableService.class, GrpcService.class)
			.entrySet()
			.stream()
			.map((e) -> new ServerServiceDefinitionSpec(e.getKey(), this.serviceInfo(e.getValue())))
			.toList();
	}

	@Override
	public List<String> listServiceNames() {
		return ApplicationContextBeanLookupUtils
			.getOrderedBeansWithAnnotation(this.applicationContext, BindableService.class, GrpcService.class)
			.keySet()
			.stream()
			.map(BindableService::bindService)
			.map(ServerServiceDefinition::getServiceDescriptor)
			.map(ServiceDescriptor::getName)
			.toList();
	}

	@Nullable
	private GrpcServiceInfo serviceInfo(@Nullable GrpcService grpcService) {
		return grpcService != null ? GrpcServiceInfo.from(grpcService) : null;
	}

}
