/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

public final class PreAuthConfigurer<H extends SecurityBuilder<AuthenticationProcessInterceptor>>
		extends SecurityConfigurerAdapter<AuthenticationProcessInterceptor, H> {

	private final ApplicationContext context;

	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	private @Nullable UserDetailsService userDetailsService;

	public PreAuthConfigurer(AuthenticationManagerBuilder authenticationManagerBuilder, ApplicationContext context) {
		this.authenticationManagerBuilder = authenticationManagerBuilder;
		this.context = context;
	}

	public PreAuthConfigurer<H> userDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
		return this;
	}

	@Override
	public void configure(H builder) {
		PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
		UserDetailsService userDetailsService = this.userDetailsService;
		if (userDetailsService == null) {
			userDetailsService = this.authenticationManagerBuilder.getDefaultUserDetailsService();
		}
		if (userDetailsService == null) {
			userDetailsService = this.context.getBean(UserDetailsService.class);
		}
		UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> details = new UserDetailsByNameServiceWrapper<>(
				userDetailsService);
		provider.setPreAuthenticatedUserDetailsService(details);
		this.authenticationManagerBuilder.authenticationProvider(provider);
	}

}
