/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.advice;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a gRPC exception handler within a {@link GrpcAdvice @GrpcAdvice}
 * class. The method should return {@link io.grpc.Status},
 * {@link io.grpc.StatusException}, or {@link io.grpc.StatusRuntimeException}.
 *
 * @author Oleksandr Shevchenko
 * @see GrpcAdvice
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GrpcExceptionHandler {

	/**
	 * Exception types to handle. If empty, inferred from method parameter types.
	 * @return the exception types to handle
	 */
	Class<? extends Throwable>[] value() default {};

}
