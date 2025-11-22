/*
 * Copyright 2024-present the original author or authors.
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
