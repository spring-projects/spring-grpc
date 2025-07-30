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
