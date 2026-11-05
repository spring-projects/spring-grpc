/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
