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

import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.util.Assert;

public class OAuth2ResourceServerConfigurer
		extends SecurityConfigurerAdapter<AuthenticationProcessInterceptor, GrpcSecurity> {

	private final ApplicationContext context;

	private JwtConfigurer jwtConfigurer;

	private OpaqueTokenConfigurer opaqueTokenConfigurer;

	public OAuth2ResourceServerConfigurer(ApplicationContext context) {
		this.context = context;
	}

	public OAuth2ResourceServerConfigurer jwt(Customizer<JwtConfigurer> jwtCustomizer) {
		if (this.jwtConfigurer == null) {
			this.jwtConfigurer = new JwtConfigurer(this.context);
		}
		jwtCustomizer.customize(this.jwtConfigurer);
		return this;
	}

	public OAuth2ResourceServerConfigurer opaqueToken(Customizer<OpaqueTokenConfigurer> opaqueTokenCustomizer) {
		if (this.opaqueTokenConfigurer == null) {
			this.opaqueTokenConfigurer = new OpaqueTokenConfigurer(this.context);
		}
		opaqueTokenCustomizer.customize(this.opaqueTokenConfigurer);
		return this;
	}

	@Override
	public void init(GrpcSecurity grpc) {
		AuthenticationProvider authenticationProvider = getAuthenticationProvider();
		if (authenticationProvider != null) {
			grpc.authenticationProvider(authenticationProvider);
		}
	}

	AuthenticationProvider getAuthenticationProvider() {
		if (this.jwtConfigurer != null) {
			return this.jwtConfigurer.getAuthenticationProvider();
		}
		if (this.opaqueTokenConfigurer != null) {
			return this.opaqueTokenConfigurer.getAuthenticationProvider();
		}
		return null;
	}

	public class JwtConfigurer {

		private final ApplicationContext context;

		private AuthenticationManager authenticationManager;

		private JwtDecoder decoder;

		private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter;

		JwtConfigurer(ApplicationContext context) {
			this.context = context;
		}

		public JwtConfigurer authenticationManager(AuthenticationManager authenticationManager) {
			Assert.notNull(authenticationManager, "authenticationManager cannot be null");
			this.authenticationManager = authenticationManager;
			return this;
		}

		public JwtConfigurer decoder(JwtDecoder decoder) {
			this.decoder = decoder;
			return this;
		}

		public JwtConfigurer jwkSetUri(String uri) {
			this.decoder = NimbusJwtDecoder.withJwkSetUri(uri).build();
			return this;
		}

		public JwtConfigurer jwtAuthenticationConverter(
				Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter) {
			this.jwtAuthenticationConverter = jwtAuthenticationConverter;
			return this;
		}

		Converter<Jwt, ? extends AbstractAuthenticationToken> getJwtAuthenticationConverter() {
			if (this.jwtAuthenticationConverter == null) {
				if (this.context.getBeanNamesForType(JwtAuthenticationConverter.class).length > 0) {
					this.jwtAuthenticationConverter = this.context.getBean(JwtAuthenticationConverter.class);
				}
				else {
					this.jwtAuthenticationConverter = new JwtAuthenticationConverter();
				}
			}
			return this.jwtAuthenticationConverter;
		}

		JwtDecoder getJwtDecoder() {
			if (this.decoder == null) {
				return this.context.getBean(JwtDecoder.class);
			}
			return this.decoder;
		}

		AuthenticationProvider getAuthenticationProvider() {
			if (this.authenticationManager != null) {
				return null;
			}
			JwtDecoder decoder = getJwtDecoder();
			Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter = getJwtAuthenticationConverter();
			JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
			provider.setJwtAuthenticationConverter(jwtAuthenticationConverter);
			return postProcess(provider);
		}

		AuthenticationManager getAuthenticationManager(GrpcSecurity grpc) {
			if (this.authenticationManager != null) {
				return this.authenticationManager;
			}
			return grpc.getSharedObject(AuthenticationManager.class);
		}

	}

	public class OpaqueTokenConfigurer {

		private final ApplicationContext context;

		private AuthenticationManager authenticationManager;

		private String introspectionUri;

		private String clientId;

		private String clientSecret;

		private Supplier<OpaqueTokenIntrospector> introspector;

		private OpaqueTokenAuthenticationConverter authenticationConverter;

		OpaqueTokenConfigurer(ApplicationContext context) {
			this.context = context;
		}

		public OpaqueTokenConfigurer authenticationManager(AuthenticationManager authenticationManager) {
			Assert.notNull(authenticationManager, "authenticationManager cannot be null");
			this.authenticationManager = authenticationManager;
			return this;
		}

		public OpaqueTokenConfigurer introspectionUri(String introspectionUri) {
			Assert.notNull(introspectionUri, "introspectionUri cannot be null");
			this.introspectionUri = introspectionUri;
			this.introspector = () -> SpringOpaqueTokenIntrospector.withIntrospectionUri(this.introspectionUri)
				.clientId(this.clientId)
				.clientSecret(this.clientSecret)
				.build();
			return this;
		}

		public OpaqueTokenConfigurer introspectionClientCredentials(String clientId, String clientSecret) {
			Assert.notNull(clientId, "clientId cannot be null");
			Assert.notNull(clientSecret, "clientSecret cannot be null");
			this.clientId = clientId;
			this.clientSecret = clientSecret;
			this.introspector = () -> SpringOpaqueTokenIntrospector.withIntrospectionUri(this.introspectionUri)
				.clientId(this.clientId)
				.clientSecret(this.clientSecret)
				.build();
			return this;
		}

		public OpaqueTokenConfigurer introspector(OpaqueTokenIntrospector introspector) {
			Assert.notNull(introspector, "introspector cannot be null");
			this.introspector = () -> introspector;
			return this;
		}

		public OpaqueTokenConfigurer authenticationConverter(
				OpaqueTokenAuthenticationConverter authenticationConverter) {
			Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
			this.authenticationConverter = authenticationConverter;
			return this;
		}

		OpaqueTokenIntrospector getIntrospector() {
			if (this.introspector != null) {
				return this.introspector.get();
			}
			return this.context.getBean(OpaqueTokenIntrospector.class);
		}

		OpaqueTokenAuthenticationConverter getAuthenticationConverter() {
			if (this.authenticationConverter != null) {
				return this.authenticationConverter;
			}
			if (this.context.getBeanNamesForType(OpaqueTokenAuthenticationConverter.class).length > 0) {
				return this.context.getBean(OpaqueTokenAuthenticationConverter.class);
			}
			return null;
		}

		AuthenticationProvider getAuthenticationProvider() {
			if (this.authenticationManager != null) {
				return null;
			}
			OpaqueTokenIntrospector introspector = getIntrospector();
			OpaqueTokenAuthenticationProvider opaqueTokenAuthenticationProvider = new OpaqueTokenAuthenticationProvider(
					introspector);
			OpaqueTokenAuthenticationConverter authenticationConverter = getAuthenticationConverter();
			if (authenticationConverter != null) {
				opaqueTokenAuthenticationProvider.setAuthenticationConverter(authenticationConverter);
			}
			return opaqueTokenAuthenticationProvider;
		}

		AuthenticationManager getAuthenticationManager(GrpcSecurity http) {
			if (this.authenticationManager != null) {
				return this.authenticationManager;
			}
			return http.getSharedObject(AuthenticationManager.class);
		}

	}

}
