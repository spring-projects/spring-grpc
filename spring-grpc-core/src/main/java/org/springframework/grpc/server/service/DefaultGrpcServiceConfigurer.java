/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.util.Assert;

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

	private List<ServerInterceptor> globalInterceptors = new ArrayList<>();

	public DefaultGrpcServiceConfigurer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		this.globalInterceptors.addAll(findGlobalInterceptors());
	}

	@Override
	public ServerServiceDefinition configure(GrpcServiceSpec serviceSpec, @Nullable GrpcServerFactory serverFactory) {
		Assert.notNull(serviceSpec, () -> "serviceSpec must not be null");
		return bindInterceptors(serviceSpec.service(), serviceSpec.serviceInfo(), serverFactory);
	}

	private List<ServerInterceptor> findGlobalInterceptors() {
		return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(this.applicationContext,
				ServerInterceptor.class, GlobalServerInterceptor.class);
	}

	private ServerServiceDefinition bindInterceptors(BindableService bindableService,
			@Nullable GrpcServiceInfo serviceInfo, @Nullable GrpcServerFactory serverFactory) {
		var serviceDef = bindableService.bindService();

		// Add and filter global interceptors first
		List<ServerInterceptor> allInterceptors = new ArrayList<>(this.globalInterceptors);
		if (serverFactory != null) {
			allInterceptors.removeIf(interceptor -> !serverFactory.supports(interceptor, serviceDef));
		}
		if (serviceInfo == null) {
			return ServerInterceptors.interceptForward(serviceDef, allInterceptors);
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

}
