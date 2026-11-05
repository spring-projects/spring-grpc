/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.advice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.advice.support.AdviceVisibilitySamples;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Tests for {@link GrpcAdviceExceptionHandler}.
 *
 * @author Oleksandr Shevchenko
 * @author Jialin Chen
 */
class GrpcAdviceExceptionHandlerTests {

	@Test
	void handlesExceptionInPublicAdviceFromAnotherPackage() {
		assertAdviceHandlesException(AdviceVisibilitySamples.PublicAdvice.class);
	}

	@Test
	void handlesExceptionInPackagePrivateAdviceFromAnotherPackage() {
		assertAdviceHandlesException(AdviceVisibilitySamples.packagePrivateAdviceType());
	}

	private void assertAdviceHandlesException(Class<?> adviceType) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(adviceType,
				GrpcAdviceDiscoverer.class, GrpcExceptionHandlerMethodResolver.class,
				GrpcAdviceExceptionHandler.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);
			StatusException result = handler.handleException(new IllegalArgumentException("bad argument"));
			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
			assertThat(result.getStatus().getDescription()).isEqualTo("bad argument");
		}
	}

	@Test
	void handlesExceptionWithMappedHandler() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			StatusException result = handler.handleException(new IllegalArgumentException("bad argument"));

			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
			assertThat(result.getStatus().getDescription()).isEqualTo("bad argument");
		}
	}

	@Test
	void handlesExceptionWithExplicitType() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			StatusException result = handler.handleException(new TimeoutException("timed out"));

			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED);
			assertThat(result.getStatus().getDescription()).isEqualTo("timed out");
		}
	}

	@Test
	void returnsNullForUnmappedException() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			StatusException result = handler.handleException(new NullPointerException("unmapped"));

			assertThat(result).isNull();
		}
	}

	@Test
	void handlesStatusExceptionReturnType() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			StatusException result = handler.handleException(new IllegalStateException("state error"));

			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
		}
	}

	@Test
	void handlesStatusRuntimeExceptionWithMetadata() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			StatusException result = handler.handleException(new UnsupportedOperationException("not supported"));

			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.UNIMPLEMENTED);
			assertThat(result.getTrailers()).isNotNull();
			assertThat(result.getTrailers().get(TestAdvice.ERROR_KEY)).isEqualTo("custom-error");
		}
	}

	@Test
	void selectsMostSpecificHandler() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			GrpcAdviceExceptionHandler handler = context.getBean(GrpcAdviceExceptionHandler.class);

			// CustomRuntimeException extends RuntimeException, should use specific
			// handler
			StatusException result = handler.handleException(new CustomRuntimeException("custom"));

			assertThat(result).isNotNull();
			assertThat(result.getStatus().getCode()).isEqualTo(Status.Code.INTERNAL);
			assertThat(result.getStatus().getDescription()).isEqualTo("custom runtime: custom");
		}
	}

	@Configuration
	static class TestConfig {

		@Bean
		GrpcAdviceDiscoverer grpcAdviceDiscoverer(ApplicationContext applicationContext) {
			return new GrpcAdviceDiscoverer(applicationContext);
		}

		@Bean
		GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver(
				GrpcAdviceDiscoverer grpcAdviceDiscoverer) {
			return new GrpcExceptionHandlerMethodResolver(grpcAdviceDiscoverer);
		}

		@Bean
		GrpcAdviceExceptionHandler grpcAdviceExceptionHandler(
				GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver) {
			return new GrpcAdviceExceptionHandler(grpcExceptionHandlerMethodResolver);
		}

		@Bean
		TestAdvice testAdvice() {
			return new TestAdvice();
		}

	}

	@GrpcAdvice
	static class TestAdvice {

		static final Metadata.Key<String> ERROR_KEY = Metadata.Key.of("error-code", Metadata.ASCII_STRING_MARSHALLER);

		@GrpcExceptionHandler
		public Status handleIllegalArgument(IllegalArgumentException ex) {
			return Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).withCause(ex);
		}

		@GrpcExceptionHandler(TimeoutException.class)
		public Status handleTimeout(TimeoutException ex) {
			return Status.DEADLINE_EXCEEDED.withDescription(ex.getMessage()).withCause(ex);
		}

		@GrpcExceptionHandler
		public StatusException handleIllegalState(IllegalStateException ex) {
			return Status.FAILED_PRECONDITION.withDescription(ex.getMessage()).withCause(ex).asException();
		}

		@GrpcExceptionHandler
		public StatusRuntimeException handleUnsupported(UnsupportedOperationException ex) {
			Metadata metadata = new Metadata();
			metadata.put(ERROR_KEY, "custom-error");
			return Status.UNIMPLEMENTED.withDescription(ex.getMessage()).withCause(ex).asRuntimeException(metadata);
		}

		@GrpcExceptionHandler
		public Status handleCustomRuntime(CustomRuntimeException ex) {
			return Status.INTERNAL.withDescription("custom runtime: " + ex.getMessage()).withCause(ex);
		}

	}

	static class CustomRuntimeException extends RuntimeException {

		CustomRuntimeException(String message) {
			super(message);
		}

	}

}
