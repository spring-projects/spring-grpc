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
