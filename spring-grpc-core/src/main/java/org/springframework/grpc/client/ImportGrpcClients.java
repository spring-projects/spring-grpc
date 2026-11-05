/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to create gRPC client beans.
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
	@AliasFor("value")
	Class<?>[] types() default {};

	/**
	 * Concrete types of the stubs to create.
	 * @return the types of the stubs
	 */
	@AliasFor("types")
	Class<?>[] value() default {};

	/**
	 * The factory type to use to create the stubs. Only needed if you are scanning (with
	 * basePackageClasses or basePackages) and you need to customize the stub creation.
	 * @return the factory type, default is {@link BlockingStubFactory}
	 */
	Class<? extends StubFactory<?>> factory() default UnspecifiedStubFactory.class;

	/**
	 * The base package classes to scan for stub implementations. If not specified,
	 * scanning will be done from the package of the class with this annotation. When a
	 * stub is found, a client bean will be created for it using the configured
	 * {@link #factory()}.
	 * @return the base package classes for scanning
	 */
	Class<?>[] basePackageClasses() default {};

	/**
	 * The base packages to scan for stub implementations. When a stub is found, a client
	 * bean will be created for it using the configured {@link #factory()}.
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
