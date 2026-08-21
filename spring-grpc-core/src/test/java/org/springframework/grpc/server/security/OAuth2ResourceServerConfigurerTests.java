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

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/**
 * Tests for {@link OAuth2ResourceServerConfigurer}.
 */
class OAuth2ResourceServerConfigurerTests {

	@Test
	void jwtConfigurerCreatesJwtAuthenticationProvider() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			configurer.jwt((jwt) -> jwt.decoder(mock(JwtDecoder.class)));

			assertThat(configurer.getAuthenticationProvider()).isInstanceOf(JwtAuthenticationProvider.class);
		}
	}

	@Test
	void opaqueTokenConfigurerCreatesOpaqueTokenAuthenticationProvider() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			configurer.opaqueToken((opaqueToken) -> opaqueToken.introspector(mock(OpaqueTokenIntrospector.class)));

			assertThat(configurer.getAuthenticationProvider()).isInstanceOf(OpaqueTokenAuthenticationProvider.class);
		}
	}

	@Test
	void opaqueTokenConfigurerResolvesIntrospectorFromContext() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			OpaqueTokenIntrospector introspector = (token) -> principal("alice");
			context.registerBean(OpaqueTokenIntrospector.class, () -> introspector);
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			configurer.opaqueToken((opaqueToken) -> {
			});

			AuthenticationProvider provider = configurer.getAuthenticationProvider();
			assertThat(provider).isNotNull();
			BearerTokenAuthentication authentication = (BearerTokenAuthentication) provider
				.authenticate(new BearerTokenAuthenticationToken("opaque-token"));
			assertThat(authentication.getName()).isEqualTo("alice");
		}
	}

	@Test
	void opaqueTokenConfigurerAppliesIntrospectionUri() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			configurer.opaqueToken((opaqueToken) -> opaqueToken.introspectionUri("https://example.com/introspect")
				.introspectionClientCredentials("client", "secret"));

			assertThat(configurer.getAuthenticationProvider()).isInstanceOf(OpaqueTokenAuthenticationProvider.class);
		}
	}

	@Test
	void jwtConfigurerTakesPrecedenceOverOpaqueToken() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			configurer.jwt((jwt) -> jwt.decoder(mock(JwtDecoder.class)));
			configurer.opaqueToken((opaqueToken) -> opaqueToken.introspector(mock(OpaqueTokenIntrospector.class)));

			assertThat(configurer.getAuthenticationProvider()).isInstanceOf(JwtAuthenticationProvider.class);
		}
	}

	@Test
	void noAuthenticationProviderWhenNothingConfigured() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.refresh();
			OAuth2ResourceServerConfigurer configurer = new OAuth2ResourceServerConfigurer(context);

			assertThat(configurer.getAuthenticationProvider()).isNull();
		}
	}

	private static OAuth2AuthenticatedPrincipal principal(String name) {
		return new DefaultOAuth2AuthenticatedPrincipal(name,
				Map.of(OAuth2AccessToken.TokenType.BEARER.getValue(), name, "sub", name),
				AuthorityUtils.createAuthorityList("SCOPE_profile"));
	}

}
