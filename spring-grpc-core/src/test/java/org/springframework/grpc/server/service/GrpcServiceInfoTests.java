/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AnnotationUtils;

import io.grpc.ServerInterceptor;

/**
 * Tests for {@link GrpcServiceInfo}.
 */
class GrpcServiceInfoTests {

	@Nested
	class WithInterceptorsApiInvokedWith {

		@Test
		void nullInterceptors() {
			assertThatIllegalArgumentException().isThrownBy(() -> GrpcServiceInfo.withInterceptors(null))
				.withMessage("interceptors must not be null");
		}

		@Test
		void interceptorTypes() {
			assertThat(GrpcServiceInfo.withInterceptors(List.of(ServerInterceptor.class))).satisfies((serviceInfo) -> {
				assertThat(serviceInfo.interceptors()).containsExactly(ServerInterceptor.class);
				assertThat(serviceInfo.interceptorNames()).isEmpty();
				assertThat(serviceInfo.blendWithGlobalInterceptors()).isFalse();
			});
		}

	}

	@Nested
	class WithInterceptorNamesApiInvokedWith {

		@Test
		void nullInterceptorNames() {
			assertThatIllegalArgumentException().isThrownBy(() -> GrpcServiceInfo.withInterceptorNames(null))
				.withMessage("interceptorNames must not be null");
		}

		@Test
		void interceptorNames() {
			assertThat(GrpcServiceInfo.withInterceptorNames(List.of("myInterceptor"))).satisfies((serviceInfo) -> {
				assertThat(serviceInfo.interceptors()).isEmpty();
				assertThat(serviceInfo.interceptorNames()).containsExactly("myInterceptor");
				assertThat(serviceInfo.blendWithGlobalInterceptors()).isFalse();
			});
		}

	}

	@Nested
	class FromApiInvokedWith {

		@Test
		void nullGrpcService() {
			assertThat(GrpcServiceInfo.from(null)).isNull();
		}

		@Test
		void grpcServiceAnnotationWithDefaults() {
			var grpcServiceAnnotation = AnnotationUtils.findAnnotation(TestServiceMarkedWithDefaults.class,
					GrpcService.class);
			assertThat(GrpcServiceInfo.from(grpcServiceAnnotation)).satisfies((serviceInfo) -> {
				assertThat(serviceInfo.interceptors()).isEmpty();
				assertThat(serviceInfo.interceptorNames()).isEmpty();
				assertThat(serviceInfo.blendWithGlobalInterceptors()).isFalse();
			});
		}

		@Test
		void grpcServiceAnnotationWithAttributes() {
			var grpcServiceAnnotation = AnnotationUtils.findAnnotation(TestServiceMarkedWithAttributes.class,
					GrpcService.class);
			assertThat(GrpcServiceInfo.from(grpcServiceAnnotation)).satisfies((serviceInfo) -> {
				assertThat(serviceInfo.interceptors()).containsExactly(ServerInterceptor.class);
				assertThat(serviceInfo.interceptorNames()).containsExactly("myInterceptor");
				assertThat(serviceInfo.blendWithGlobalInterceptors()).isTrue();
			});
		}

	}

	@GrpcService
	static class TestServiceMarkedWithDefaults {

	}

	@GrpcService(interceptors = ServerInterceptor.class, interceptorNames = "myInterceptor",
			blendWithGlobalInterceptors = true)
	static class TestServiceMarkedWithAttributes {

	}

}
