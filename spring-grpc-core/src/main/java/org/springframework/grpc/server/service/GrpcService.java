/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
