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

package org.springframework.grpc.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;
import io.grpc.stub.AbstractStub;

/**
 * Annotation for fields of type {@link Channel} or subclasses of {@link AbstractStub}
 * (gRPC client stubs). Also works for annotated methods that only take a single parameter
 * of these types. Annotated fields/methods will be automatically populated/invoked by
 * Spring.
 *
 * @author Oleksandr Shevchenko
 * @see GrpcClientBeanPostProcessor
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GrpcClient {

	/**
	 * The name of the gRPC channel/client.
	 * @return the name of the gRPC channel/client
	 */
	String value() default "default";

	/**
	 * The {@link ClientInterceptor} bean types to be applied to this client.
	 * @return the interceptor bean types to be applied to this client
	 */
	Class<? extends ClientInterceptor>[] interceptors() default {};

	/**
	 * The {@link ClientInterceptor} bean names to be applied to this client.
	 * @return the interceptor bean names to be applied to this client
	 */
	String[] interceptorNames() default {};

	/**
	 * Whether the client-specific interceptors should be blended with the global
	 * interceptors.
	 * @return whether the client-specific interceptors should be blended with the global
	 * interceptors
	 */
	boolean blendWithGlobalInterceptors() default false;

}
