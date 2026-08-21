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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.springframework.grpc.internal.GrpcHeaders;
import org.springframework.security.core.Authentication;

import com.google.protobuf.Empty;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

/**
 * Tests for {@link HttpBasicAuthenticationExtractor}.
 */
class HttpBasicAuthenticationExtractorTests {

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName("Simple/SayHello")
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	private final HttpBasicAuthenticationExtractor extractor = new HttpBasicAuthenticationExtractor();

	@Test
	void extractsUsernameAndPassword() {
		Authentication authentication = this.extractor.extract(basicHeaders("user:secret"), Attributes.EMPTY, METHOD);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("user");
		assertThat(authentication.getCredentials()).isEqualTo("secret");
	}

	@Test
	void extractsIgnoringSchemeCase() {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY,
				"BaSiC " + Base64.getEncoder().encodeToString("user:secret".getBytes(StandardCharsets.UTF_8)));

		Authentication authentication = this.extractor.extract(headers, Attributes.EMPTY, METHOD);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("user");
	}

	@Test
	void returnsNullWhenHeaderMissing() {
		assertThat(this.extractor.extract(new Metadata(), Attributes.EMPTY, METHOD)).isNull();
	}

	@Test
	void returnsNullWhenSchemeIsNotBasic() {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY, "Bearer token-value");

		assertThat(this.extractor.extract(headers, Attributes.EMPTY, METHOD)).isNull();
	}

	@Test
	void returnsNullWhenCredentialsHaveNoSeparator() {
		assertThat(this.extractor.extract(basicHeaders("nocolon"), Attributes.EMPTY, METHOD)).isNull();
	}

	private static Metadata basicHeaders(String credentials) {
		Metadata headers = new Metadata();
		headers.put(GrpcHeaders.AUTHORIZATION_KEY,
				"Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
		return headers;
	}

}
