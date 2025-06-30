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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.ServerInterceptorFilter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

/**
 * Default {@link GrpcServiceConfigurer} that binds and configures services with
 * interceptors.
 *
 * @author Chris Bono
 */
public class DefaultGrpcServiceConfigurer implements GrpcServiceConfigurer, InitializingBean {

	private final LogAccessor log = new LogAccessor(getClass());

	private final ApplicationContext applicationContext;

	private List<ServerInterceptor> globalInterceptors;

	private ServerInterceptorFilter interceptorFilter;

	public DefaultGrpcServiceConfigurer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		this.globalInterceptors = findGlobalInterceptors();
		this.interceptorFilter = findInterceptorFilter();
	}

	@Override
	public ServerServiceDefinition configure(GrpcServerFactory serverFactory, BindableService bindableService,
			@Nullable GrpcServiceInfo serviceInfo) {
		Assert.notNull(serverFactory, () -> "serverFactory must not be null");
		Assert.notNull(bindableService, () -> "bindableService must not be null");
		return bindInterceptors(serverFactory, bindableService, serviceInfo);
	}

	private List<ServerInterceptor> findGlobalInterceptors() {
		return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(this.applicationContext,
				ServerInterceptor.class, GlobalServerInterceptor.class);
	}

	private ServerServiceDefinition bindInterceptors(GrpcServerFactory serverFactory, BindableService bindableService,
			@Nullable GrpcServiceInfo serviceInfo) {
		var serviceDef = bindableService.bindService();
		if (serviceInfo == null) {
			return ServerInterceptors.interceptForward(serviceDef, this.globalInterceptors);
		}
		// Add and filter global interceptors first
		List<ServerInterceptor> allInterceptors = new ArrayList<>(this.globalInterceptors);
		if (this.interceptorFilter != null) {
			allInterceptors
				.removeIf(interceptor -> !this.interceptorFilter.filter(interceptor, serverFactory, bindableService));
		}
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

	private ServerInterceptorFilter findInterceptorFilter() {
		try {
			return this.applicationContext.getBean(ServerInterceptorFilter.class);
		}
		catch (NoUniqueBeanDefinitionException noUniqueBeanEx) {
			this.log.warn(noUniqueBeanEx,
					() -> "No unique ServerInterceptorFilter bean found. Consider defining a single bean or marking one as @Primary");
			return null;
		}
		catch (NoSuchBeanDefinitionException ignored) {
			this.log.debug(
					() -> "No ServerInterceptorFilter bean found - filtering will not be applied to server interceptors.");
			return null;
		}
	}

}
