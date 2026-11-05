/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

import io.grpc.ServerInterceptor;

/**
 * Additional information about a gRPC service that can be used when configuring the
 * service.
 *
 * @param interceptors list of {@link ServerInterceptor} bean types to be applied to the
 * service
 * @param interceptorNames list of {@link ServerInterceptor} bean names to be applied to
 * the service
 * @param blendWithGlobalInterceptors whether the service specific interceptors should be
 * merged and sorted (blended) with the global interceptors
 * @author Chris Bono
 */
public record GrpcServiceInfo(Class<? extends ServerInterceptor>[] interceptors, String[] interceptorNames,
		boolean blendWithGlobalInterceptors) {

	public GrpcServiceInfo {
		Assert.notNull(interceptors, "interceptors must not be null");
		Assert.notNull(interceptorNames, "interceptorNames must not be null");
	}

	/**
	 * Construct a service info from a {@link GrpcService} annotation.
	 * @param grpcService the service annotation
	 * @return the service info or null if the supplied annotation is null
	 */
	@Nullable
	public static GrpcServiceInfo from(@Nullable GrpcService grpcService) {
		return grpcService != null ? new GrpcServiceInfo(grpcService.interceptors(), grpcService.interceptorNames(),
				grpcService.blendWithGlobalInterceptors()) : null;
	}

	/**
	 * Construct a service info with the specified interceptors.
	 * @param interceptors non-null list of interceptor bean types
	 * @return the service info with the supplied interceptors
	 */
	@SuppressWarnings("unchecked")
	public static GrpcServiceInfo withInterceptors(List<Class<? extends ServerInterceptor>> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new GrpcServiceInfo(interceptors.toArray(new Class[0]), new String[0], false);
	}

	/**
	 * Construct a service info with the specified interceptors names.
	 * @param interceptorNames non-null list of interceptor bean names
	 * @return the service info with the supplied interceptors names
	 */
	@SuppressWarnings("unchecked")
	public static GrpcServiceInfo withInterceptorNames(List<String> interceptorNames) {
		Assert.notNull(interceptorNames, "interceptorNames must not be null");
		return new GrpcServiceInfo(new Class[0], interceptorNames.toArray(new String[0]), false);
	}

}
