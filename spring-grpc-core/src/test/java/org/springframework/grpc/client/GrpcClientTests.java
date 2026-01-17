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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.grpc.Channel;
import io.grpc.ClientInterceptor;

/**
 * Tests for {@link GrpcClient}.
 *
 */
class GrpcClientTests {

	@Test
	void defaultValues() throws Exception {
		GrpcClient annotation = getAnnotation("defaultChannel");

		assertThat(annotation.value()).isEqualTo("default");
		assertThat(annotation.interceptors()).isEmpty();
		assertThat(annotation.interceptorNames()).isEmpty();
		assertThat(annotation.blendWithGlobalInterceptors()).isFalse();
	}

	@Test
	void customChannel() throws Exception {
		GrpcClient annotation = getAnnotation("customChannel");

		assertThat(annotation.value()).isEqualTo("my-channel");
	}

	@Test
	void withInterceptorTypes() throws Exception {
		GrpcClient annotation = getAnnotation("withInterceptors");

		assertThat(annotation.interceptors()).containsExactly(TestInterceptor.class);
	}

	@Test
	void withInterceptorNames() throws Exception {
		GrpcClient annotation = getAnnotation("withInterceptorNames");

		assertThat(annotation.interceptorNames()).containsExactly("interceptor1", "interceptor2");
	}

	@Test
	void withBlendInterceptors() throws Exception {
		GrpcClient annotation = getAnnotation("withBlend");

		assertThat(annotation.blendWithGlobalInterceptors()).isTrue();
	}

	private GrpcClient getAnnotation(String fieldName) throws Exception {
		Field field = TestBean.class.getDeclaredField(fieldName);
		return field.getAnnotation(GrpcClient.class);
	}

	static class TestBean {

		@GrpcClient
		Channel defaultChannel;

		@GrpcClient("my-channel")
		Channel customChannel;

		@GrpcClient(interceptors = TestInterceptor.class)
		Channel withInterceptors;

		@GrpcClient(interceptorNames = { "interceptor1", "interceptor2" })
		Channel withInterceptorNames;

		@GrpcClient(blendWithGlobalInterceptors = true)
		Channel withBlend;

	}

	static class TestInterceptor implements ClientInterceptor {

		@Override
		public <ReqT, RespT> io.grpc.ClientCall<ReqT, RespT> interceptCall(io.grpc.MethodDescriptor<ReqT, RespT> method,
				io.grpc.CallOptions callOptions, Channel next) {
			return next.newCall(method, callOptions);
		}

	}

}
