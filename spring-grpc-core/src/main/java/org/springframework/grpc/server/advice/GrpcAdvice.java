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

import org.springframework.stereotype.Component;

/**
 * Marks a class for gRPC exception handling with
 * {@link GrpcExceptionHandler @GrpcExceptionHandler} methods.
 *
 * @author Oleksandr Shevchenko
 * @see GrpcExceptionHandler
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GrpcAdvice {

}
