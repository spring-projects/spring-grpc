/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.grpc.internal.ApplicationContextBeanLookupUtils;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;

/**
 * Configure a {@link ManagedChannelBuilder} with client interceptors.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 */
public class ClientInterceptorsConfigurer implements InitializingBean {

	private final ApplicationContext applicationContext;

	private List<ClientInterceptor> globalInterceptors = new ArrayList<>();

	public ClientInterceptorsConfigurer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Configure a {@link ManagedChannelBuilder} with client interceptors.
	 * @param builder the builder to configure
	 * @param interceptors the non-null list of interceptors to be applied to the channel
	 * @param mergeWithGlobalInterceptors whether the provided interceptors should be
	 * blended with the global interceptors.
	 * @param factory the channel factory used to filter global interceptors
	 */
	protected void configureInterceptors(ManagedChannelBuilder<?> builder, List<ClientInterceptor> interceptors,
			boolean mergeWithGlobalInterceptors, GrpcChannelFactory factory) {
		// Add global interceptors first
		List<ClientInterceptor> allInterceptors = new ArrayList<>(this.globalInterceptors);
		// Add specific interceptors
		allInterceptors.addAll(interceptors);
		// Filter all interceptors
		allInterceptors.removeIf(interceptor -> !factory.supports(interceptor));
		if (mergeWithGlobalInterceptors) {
			ApplicationContextBeanLookupUtils.sortBeansIncludingOrderAnnotation(this.applicationContext,
					ClientInterceptor.class, allInterceptors);
		}
		Collections.reverse(allInterceptors);
		builder.intercept(allInterceptors);
	}

	@Override
	public void afterPropertiesSet() {
		this.globalInterceptors = findGlobalInterceptors();
	}

	private List<ClientInterceptor> findGlobalInterceptors() {
		return ApplicationContextBeanLookupUtils.getBeansWithAnnotation(this.applicationContext,
				ClientInterceptor.class, GlobalClientInterceptor.class);
	}

}
