/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
