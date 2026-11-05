/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.google.protobuf.Empty;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;

/**
 * Tests for {@link DelegatingGrpcAuthenticationExtractor}.
 */
class DelegatingGrpcAuthenticationExtractorTests {

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName("Simple/SayHello")
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	@Test
	void firstNonNullAuthenticationWins() {
		AtomicInteger calls = new AtomicInteger();
		GrpcAuthenticationExtractor extractor = new DelegatingGrpcAuthenticationExtractor(counting(calls, none()),
				counting(calls, named("second")), counting(calls, named("third")));

		Authentication authentication = extract(extractor);

		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("second");
		assertThat(calls).hasValue(2);
	}

	@Test
	void returnsNullWhenNoDelegateAuthenticates() {
		GrpcAuthenticationExtractor extractor = new DelegatingGrpcAuthenticationExtractor(none(), none());

		assertThat(extract(extractor)).isNull();
	}

	@Test
	void delegateExceptionIsNotCaught() {
		GrpcAuthenticationExtractor failing = (headers, attributes, method) -> {
			throw new BadCredentialsException("bad token");
		};
		GrpcAuthenticationExtractor extractor = new DelegatingGrpcAuthenticationExtractor(failing, named("fallback"));

		assertThatExceptionOfType(BadCredentialsException.class).isThrownBy(() -> extract(extractor));
	}

	@Test
	void delegateListIsCopied() {
		List<GrpcAuthenticationExtractor> delegates = new ArrayList<>(List.of(named("first")));
		GrpcAuthenticationExtractor extractor = new DelegatingGrpcAuthenticationExtractor(delegates);

		delegates.clear();
		delegates.add(named("replacement"));

		Authentication authentication = extract(extractor);
		assertThat(authentication).isNotNull();
		assertThat(authentication.getName()).isEqualTo("first");
	}

	@Test
	void emptyDelegateListIsRejected() {
		assertThatIllegalArgumentException().isThrownBy(() -> new DelegatingGrpcAuthenticationExtractor(List.of()));
	}

	@Test
	void emptyDelegateArrayIsRejected() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new DelegatingGrpcAuthenticationExtractor(new GrpcAuthenticationExtractor[0]));
	}

	private static Authentication extract(GrpcAuthenticationExtractor extractor) {
		return extractor.extract(new Metadata(), Attributes.EMPTY, METHOD);
	}

	private static GrpcAuthenticationExtractor counting(AtomicInteger calls, GrpcAuthenticationExtractor delegate) {
		return (headers, attributes, method) -> {
			calls.incrementAndGet();
			return delegate.extract(headers, attributes, method);
		};
	}

	private static GrpcAuthenticationExtractor named(String name) {
		return (headers, attributes, method) -> new TestingAuthenticationToken(name, "n/a", "ROLE_USER");
	}

	private static GrpcAuthenticationExtractor none() {
		return (headers, attributes, method) -> null;
	}

}
