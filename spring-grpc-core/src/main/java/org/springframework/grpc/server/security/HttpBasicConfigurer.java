/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.SecurityBuilder;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;

public final class HttpBasicConfigurer<H extends SecurityBuilder<AuthenticationProcessInterceptor>>
		extends SecurityConfigurerAdapter<AuthenticationProcessInterceptor, H> {

	private final ApplicationContext context;

	private final AuthenticationManagerBuilder authenticationManagerBuilder;

	private @Nullable UserDetailsService userDetailsService;

	public HttpBasicConfigurer(AuthenticationManagerBuilder authenticationManagerBuilder, ApplicationContext context) {
		this.authenticationManagerBuilder = authenticationManagerBuilder;
		this.context = context;
	}

	public HttpBasicConfigurer<H> userDetailsService(UserDetailsService userDetailsService) {
		this.userDetailsService = userDetailsService;
		return this;
	}

	@Override
	public void configure(H builder) {
		UserDetailsService userDetailsService = this.userDetailsService;
		if (userDetailsService == null) {
			userDetailsService = this.authenticationManagerBuilder.getDefaultUserDetailsService();
		}
		if (userDetailsService == null) {
			userDetailsService = this.context.getBean(UserDetailsService.class);
		}
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		this.authenticationManagerBuilder.authenticationProvider(provider);
	}

}
