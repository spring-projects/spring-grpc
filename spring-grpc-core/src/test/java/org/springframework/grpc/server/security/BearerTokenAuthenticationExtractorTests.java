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

package org.springframework.grpc.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.grpc.internal.GrpcHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import com.google.protobuf.Empty;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

/**
 * Tests for {@link BearerTokenAuthenticationExtractor}.
 */
class BearerTokenAuthenticationExtractorTests {

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName("Simple/SayHello")
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	private final BearerTokenAuthenticationExtractor extractor = new BearerTokenAuthenticationExtractor();

	@Test
	void extractsBearerToken() {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY, "Bearer token-value");

		Authentication authentication = this.extractor.extract(headers, Attributes.EMPTY, METHOD);

		assertThat(authentication).isInstanceOf(BearerTokenAuthenticationToken.class);
		assertThat(((BearerTokenAuthenticationToken) authentication).getToken()).isEqualTo("token-value");
	}

	@Test
	void extractsBearerTokenIgnoringSchemeCase() {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY, "bEaReR token-value");

		Authentication authentication = this.extractor.extract(headers, Attributes.EMPTY, METHOD);

		assertThat(authentication).isNotNull();
		assertThat(((BearerTokenAuthenticationToken) authentication).getToken()).isEqualTo("token-value");
	}

	@Test
	void returnsNullWhenHeaderMissing() {
		assertThat(this.extractor.extract(new Metadata(), Attributes.EMPTY, METHOD)).isNull();
	}

	@Test
	void returnsNullWhenSchemeIsNotBearer() {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY, "Basic dXNlcjpwYXNz");

		assertThat(this.extractor.extract(headers, Attributes.EMPTY, METHOD)).isNull();
	}

}
