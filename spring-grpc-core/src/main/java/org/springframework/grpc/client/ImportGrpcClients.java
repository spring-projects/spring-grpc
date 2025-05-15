/*
 * Copyright 2024-2024 the original author or authors.
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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Annotation to create gRPC client beans. If you want more control over the creation of
 * the clients, or you don't want to use the annotation, you can use a bean of type
 * {@link GrpcClientFactoryCustomizer} instead.
 *
 * @author Dave Syer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(AnnotationGrpcClientRegistrar.class)
@Repeatable(ImportGrpcClients.Container.class)
public @interface ImportGrpcClients {

	/**
	 * The name or base URL of the gRPC server to connect to. If not specified, the client
	 * will connect to the default server.
	 * @return the name or base URL of the server
	 */
	String target() default "default";

	/**
	 * The prefix to use when creating bean definitions for clients, completed by the
	 * simple name of the class. Default is empty. You only need to specify a value if you
	 * define more than one client of the same type (or with types that have the same name
	 * in different packages).
	 * @return the prefix
	 */
	String prefix() default "";

	/**
	 * Concrete types of the stubs to create.
	 * @return the types of the stubs
	 */
	Class<?>[] types() default {};

	/**
	 * The factory type to use to create the stubs. Only needed if you are scanning (with
	 * basePackageClasses or basePackages) and you need to customize the stub creation.
	 * @return the factory type, default is {@link BlockingStubFactory}
	 */
	Class<? extends StubFactory<?>> factory() default UnspecifiedStubFactory.class;

	/**
	 * The base package classes to scan for annotated components. If not specified,
	 * scanning will be done from the package of the class with this annotation.
	 * @return the base package classes for scanning
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * The base packages to scan for annotated components.
	 * @return the base packages for scanning
	 */
	String[] basePackages() default {};

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	// In case there is more than one @ImportGrpcClients annotation we need to
	// import here as well:
	@Import(AnnotationGrpcClientRegistrar.class)
	@interface Container {

		ImportGrpcClients[] value() default {};

	}

}
