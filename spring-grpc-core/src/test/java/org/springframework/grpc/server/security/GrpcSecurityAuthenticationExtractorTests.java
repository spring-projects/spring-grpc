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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthenticatedAuthorizationManager;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;

import com.google.protobuf.Empty;
import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.protobuf.ProtoUtils;

/**
 * Tests for the composition of the {@link GrpcAuthenticationExtractor} instances
 * registered on {@link GrpcSecurity}.
 */
class GrpcSecurityAuthenticationExtractorTests {

	private static final MethodDescriptor<Empty, Empty> METHOD = MethodDescriptor.<Empty, Empty>newBuilder()
		.setType(MethodDescriptor.MethodType.UNARY)
		.setFullMethodName("Simple/SayHello")
		.setRequestMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.setResponseMarshaller(ProtoUtils.marshaller(Empty.getDefaultInstance()))
		.build();

	@Test
	void firstNonNullExtractorWins() {
		assertThat(authenticate(none(), named("first"), named("second"))).isEqualTo("first");
	}

	@Test
	void unauthenticatedWhenNoExtractorMatches() {
		assertThat(authenticate(none(), none())).isNull();
	}

	@Test
	void extractorsAreAppliedInAnnotatedOrder() {
		assertThat(authenticate(new LateExtractor(), new EarlyExtractor())).isEqualTo("early");
	}

	/**
	 * Drives a call through the interceptor built by {@link GrpcSecurity} and reports the
	 * name of the {@link Authentication} the extractors produced, or {@code null} if the
	 * call was left unauthenticated.
	 */
	private static String authenticate(GrpcAuthenticationExtractor... extractors) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			GrpcSecurity grpc = new GrpcSecurity(ObjectPostProcessor.identity(),
					new AuthenticationManagerBuilder(ObjectPostProcessor.identity()), context);
			AtomicReference<String> authenticated = new AtomicReference<>();
			grpc.authenticationManager((authentication) -> {
				authenticated.set(authentication.getName());
				return authentication;
			});
			grpc.authorizationManager(AuthenticatedAuthorizationManager.authenticated());
			for (GrpcAuthenticationExtractor extractor : extractors) {
				grpc.authenticationExtractor(extractor);
			}

			AuthenticationProcessInterceptor interceptor = grpc.build();
			@SuppressWarnings("unchecked")
			ServerCall<Empty, Empty> call = mock(ServerCall.class);
			when(call.getAttributes()).thenReturn(Attributes.EMPTY);
			when(call.getMethodDescriptor()).thenReturn(METHOD);
			@SuppressWarnings("unchecked")
			ServerCallHandler<Empty, Empty> next = mock(ServerCallHandler.class);
			try {
				interceptor.interceptCall(call, new Metadata(), next);
			}
			catch (RuntimeException ex) {
				// an unauthenticated call is rejected before any extractor result is
				// recorded
			}
			return authenticated.get();
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static GrpcAuthenticationExtractor named(String name) {
		return (headers, attributes, method) -> new TestingAuthenticationToken(name, "n/a", "ROLE_USER");
	}

	private static GrpcAuthenticationExtractor none() {
		return (headers, attributes, method) -> null;
	}

	@Order(1)
	static class EarlyExtractor implements GrpcAuthenticationExtractor {

		@Override
		public Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
			return new TestingAuthenticationToken("early", "n/a", "ROLE_USER");
		}

	}

	@Order(2)
	static class LateExtractor implements GrpcAuthenticationExtractor {

		@Override
		public Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {
			return new TestingAuthenticationToken("late", "n/a", "ROLE_USER");
		}

	}

}
