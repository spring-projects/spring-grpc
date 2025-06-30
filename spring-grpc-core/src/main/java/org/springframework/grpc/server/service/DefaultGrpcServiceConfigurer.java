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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

/**
 * Default {@link GrpcServiceConfigurer} implementation that binds and configures services
 * with interceptors.
 *
 * @author Chris Bono
 */
public class DefaultGrpcServiceConfigurer implements GrpcServiceConfigurer, InitializingBean {

	private final ApplicationContext applicationContext;

	private List<ServerInterceptor> globalInterceptors;

	public DefaultGrpcServiceConfigurer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		this.globalInterceptors = findGlobalInterceptors();
	}

	@Override
	public ServerServiceDefinition configure(ServerServiceDefinitionSpec serviceDefinitionSpec) {
		return bindInterceptors(serviceDefinitionSpec.service(), serviceDefinitionSpec.serviceInfo());
	}

	private List<ServerInterceptor> findGlobalInterceptors() {
		return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(this.applicationContext,
				ServerInterceptor.class, GlobalServerInterceptor.class);
	}

	private ServerServiceDefinition bindInterceptors(BindableService bindableService,
			@Nullable GrpcServiceInfo serviceInfo) {
		var serviceDef = bindableService.bindService();
		if (serviceInfo == null) {
			return ServerInterceptors.interceptForward(serviceDef, this.globalInterceptors);
		}
		// Add global interceptors first
		List<ServerInterceptor> allInterceptors = new ArrayList<>(this.globalInterceptors);
		// Add interceptors by type
		Arrays.stream(serviceInfo.interceptors())
			.forEachOrdered(
					(interceptorClass) -> allInterceptors.add(this.applicationContext.getBean(interceptorClass)));
		// Add interceptors by name
		Arrays.stream(serviceInfo.interceptorNames())
			.forEachOrdered((interceptorBeanName) -> allInterceptors
				.add(this.applicationContext.getBean(interceptorBeanName, ServerInterceptor.class)));
		if (serviceInfo.blendWithGlobalInterceptors()) {
			ApplicationContextBeanLookupUtils.sortBeansIncludingOrderAnnotation(this.applicationContext,
					ServerInterceptor.class, allInterceptors);
		}
		return ServerInterceptors.interceptForward(serviceDef, allInterceptors);
	}

}
