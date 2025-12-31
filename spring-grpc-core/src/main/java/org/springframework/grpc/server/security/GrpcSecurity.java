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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ObservationAuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.AbstractConfiguredSecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.Assert;

import io.grpc.Attributes;
import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.micrometer.observation.ObservationRegistry;

/**
 * The <code>GrpcSecurity</code> class is responsible for configuring the security
 * settings for a gRPC server. It provides methods to configure authentication providers,
 * user details service, and authentication extractors.
 *
 * The class also defines some static constants, such as the
 * <code>AUTHORIZATION_KEY</code> can be used in your security configuration.
 *
 * The class also provides various methods to configure different authentication
 * mechanisms, such as pre-authentication, HTTP basic authentication, and custom
 * authentication extractors.
 *
 * @author Dave Syer
 */
public final class GrpcSecurity
		extends AbstractConfiguredSecurityBuilder<AuthenticationProcessInterceptor, GrpcSecurity> {

	/**
	 * A constant key used for storing and retrieving the "Authorization" header from gRPC
	 * metadata. This key is used to handle authorization information in gRPC requests.
	 *
	 * <p>
	 * The key is defined with the name "Authorization" and uses the ASCII string
	 * marshaller for encoding and decoding the header value.
	 * </p>
	 */
	public static final Metadata.Key<String> AUTHORIZATION_KEY = Metadata.Key.of("Authorization",
			Metadata.ASCII_STRING_MARSHALLER);

	/**
	 * The order value for the context filter in the gRPC security framework. This
	 * constant defines the position of the context filter in the filter chain. A lower
	 * value indicates higher precedence.
	 */
	public static final int CONTEXT_FILTER_ORDER = 0;

	/**
	 * Key for the SecurityContext in the gRPC Context.
	 */
	public static Context.Key<SecurityContext> SECURITY_CONTEXT_KEY = Context.key("spring-security-context");

	private @Nullable AuthenticationManager authenticationManager;

	private List<GrpcAuthenticationExtractor> authenticationExtractors = new ArrayList<>();

	private @Nullable AuthorizationManager<CallContext> authorizationManager;

	public GrpcSecurity(ObjectPostProcessor<Object> objectPostProcessor,
			AuthenticationManagerBuilder authenticationBuilder, ApplicationContext context) {
		super(objectPostProcessor);
		setSharedObject(AuthenticationManagerBuilder.class, authenticationBuilder);
		setSharedObject(ApplicationContext.class, context);
	}

	private ObservationRegistry getObservationRegistry() {
		ApplicationContext context = getContext();
		String[] names = context.getBeanNamesForType(ObservationRegistry.class);
		if (names.length == 1) {
			return (ObservationRegistry) context.getBean(names[0]);
		}
		return ObservationRegistry.NOOP;
	}

	private ApplicationContext getContext() {
		return getSharedObject(ApplicationContext.class);
	}

	@Override
	protected AuthenticationProcessInterceptor performBuild() {
		Objects.requireNonNull(this.authorizationManager, "authorizationManager must not null");
		if (this.authenticationManager != null) {
			setSharedObject(AuthenticationManager.class, this.authenticationManager);
		}
		else {
			ObservationRegistry registry = getObservationRegistry();
			AuthenticationManager manager = getAuthenticationManager();
			if (!registry.isNoop() && manager != null) {
				setSharedObject(AuthenticationManager.class, new ObservationAuthenticationManager(registry, manager));
			}
			else {
				setSharedObject(AuthenticationManager.class, manager);
			}
		}
		this.authenticationExtractors.sort(AnnotationAwareOrderComparator.INSTANCE);
		return new AuthenticationProcessInterceptor(getSharedObject(AuthenticationManager.class),
				new CompositeAuthenticationExtractor(this.authenticationExtractors), this.authorizationManager);
	}

	private AuthenticationManager getAuthenticationManager() {
		return getAuthenticationRegistry().getOrBuild();
	}

	public GrpcSecurity authenticationProvider(AuthenticationProvider authenticationProvider) {
		getAuthenticationRegistry().authenticationProvider(authenticationProvider);
		return this;
	}

	public GrpcSecurity userDetailsService(UserDetailsService userDetailsService) throws Exception {
		getAuthenticationRegistry().userDetailsService(userDetailsService);
		return this;
	}

	public GrpcSecurity preauth(Customizer<PreAuthConfigurer<GrpcSecurity>> customizer) throws Exception {
		customizer.customize(getOrApply(new PreAuthConfigurer<>(getAuthenticationRegistry(), getContext())));
		authenticationExtractor(new SslContextPreAuthenticationExtractor());
		return this;
	}

	public GrpcSecurity httpBasic(Customizer<HttpBasicConfigurer<GrpcSecurity>> customizer) throws Exception {
		customizer.customize(getOrApply(new HttpBasicConfigurer<>(getAuthenticationRegistry(), getContext())));
		authenticationExtractor(new HttpBasicAuthenticationExtractor());
		return this;
	}

	public GrpcSecurity authorizeRequests(Customizer<RequestMapperConfigurer> customizer) throws Exception {
		customizer.customize(getOrApply(new RequestMapperConfigurer(getContext())));
		return this;
	}

	public GrpcSecurity oauth2ResourceServer(Customizer<OAuth2ResourceServerConfigurer> customizer) throws Exception {
		customizer.customize(getOrApply(new OAuth2ResourceServerConfigurer(getContext())));
		authenticationExtractor(new BearerTokenAuthenticationExtractor());
		return this;
	}

	@SuppressWarnings({ "unchecked", "removal" })
	private <C extends SecurityConfigurerAdapter<AuthenticationProcessInterceptor, GrpcSecurity>> C getOrApply(
			C configurer) throws Exception {
		C existingConfig = (C) getConfigurer(configurer.getClass());
		if (existingConfig != null) {
			return existingConfig;
		}
		return apply(configurer);
	}

	public GrpcSecurity authenticationManager(AuthenticationManager authenticationManager) {
		Assert.notNull(authenticationManager, "authenticationManager cannot be null");
		this.authenticationManager = authenticationManager;
		return this;
	}

	public GrpcSecurity authenticationExtractor(GrpcAuthenticationExtractor authenticationExtractor) {
		Assert.notNull(authenticationExtractor, "authenticationExtractor cannot be null");
		this.authenticationExtractors.add(authenticationExtractor);
		return this;
	}

	public GrpcSecurity authorizationManager(AuthorizationManager<CallContext> authorizationManager) {
		this.authorizationManager = authorizationManager;
		return this;
	}

	private AuthenticationManagerBuilder getAuthenticationRegistry() {
		return getSharedObject(AuthenticationManagerBuilder.class);
	}

	private static class CompositeAuthenticationExtractor implements GrpcAuthenticationExtractor {

		private final List<GrpcAuthenticationExtractor> extractors;

		CompositeAuthenticationExtractor(List<GrpcAuthenticationExtractor> extractors) {
			this.extractors = extractors;
		}

		@Override
		public @Nullable Authentication extract(Metadata headers, Attributes attributes,
				MethodDescriptor<?, ?> method) {
			for (GrpcAuthenticationExtractor extractor : this.extractors) {
				Authentication authentication = extractor.extract(headers, attributes, method);
				if (authentication != null) {
					return authentication;
				}
			}
			return null;
		}

	}

}
