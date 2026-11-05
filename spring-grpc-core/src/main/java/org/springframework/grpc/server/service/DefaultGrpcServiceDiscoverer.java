/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;

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
	public List<GrpcServiceSpec> findServices() {
		return ApplicationContextBeanLookupUtils
			.getOrderedBeansWithAnnotation(this.applicationContext, BindableService.class, GrpcService.class)
			.entrySet()
			.stream()
			.map((e) -> new GrpcServiceSpec(e.getKey(), this.serviceInfo(e.getValue())))
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
