/*
 * Copyright 2016-present the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;

/**
 * Annotation that marks gRPC services that should be registered with a gRPC server.
 * <p>
 * <b>NOTE:</b> This annotation is optional as all {@link BindableService} beans will be
 * registered with a gRPC server. However, this annotation allows specifying additional
 * information about the service (e.g. interceptors).
 * <p>
 * <b>NOTE:</b> This annotation should only be added to {@link BindableService} beans.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Chris Bono
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Service
@Bean
public @interface GrpcService {

	/**
	 * The {@link ServerInterceptor} bean types to be applied to the service.
	 * @return the interceptor bean types to be applied to the service
	 */
	Class<? extends ServerInterceptor>[] interceptors() default {};

	/**
	 * The {@link ServerInterceptor} bean names to be applied to the service.
	 * @return the interceptor bean names to be applied to the service
	 */
	String[] interceptorNames() default {};

	/**
	 * Whether the service specific interceptors should be blended with the global
	 * interceptors.
	 * <p>
	 * When false, the global interceptors are applied first, followed by the service
	 * specific interceptors.
	 * <p>
	 * When true, the global interceptors are merged and sorted (blended) with the service
	 * specific interceptors.
	 * @return whether the service specific interceptors should be blended with the global
	 * interceptors
	 */
	boolean blendWithGlobalInterceptors() default false;

}
