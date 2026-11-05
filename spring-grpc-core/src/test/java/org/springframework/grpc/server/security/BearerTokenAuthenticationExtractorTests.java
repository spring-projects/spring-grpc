/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
