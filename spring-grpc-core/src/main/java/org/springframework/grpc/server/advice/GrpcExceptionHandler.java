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
