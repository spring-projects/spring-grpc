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
//CHECKSTYLE:OFF
package org.springframework.grpc.autoconfigure.server.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.server.ConditionalOnGrpcServerEnabled;
import org.springframework.grpc.autoconfigure.server.GrpcServerExecutorProvider;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.exception.GrpcExceptionHandlerAutoConfiguration;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityContextServerInterceptor;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;

import io.grpc.internal.GrpcUtil;

@ConditionalOnClass(ObjectPostProcessor.class)
@ConditionalOnGrpcServerEnabled
@AutoConfiguration(before = GrpcExceptionHandlerAutoConfiguration.class, after = SecurityAutoConfiguration.class)
@Import({ ExceptionHandlerAutoConfiguration.class, GrpcNativeSecurityConfigurerAutoConfiguration.class,
		GrpcServletSecurityConfigurerAutoConfiguration.class })
public class GrpcSecurityAutoConfiguration {

}

@Configuration(proxyBeanMethods = false)
@Import(AuthenticationConfiguration.class)
class ExceptionHandlerAutoConfiguration {

	@Bean
	public GrpcExceptionHandler accessExceptionHandler() {
		return new SecurityGrpcExceptionHandler();
	}

}

@ConditionalOnBean(ObjectPostProcessor.class)
@Configuration(proxyBeanMethods = false)
@Conditional(GrpcServerFactoryAutoConfiguration.OnNativeGrpcServerCondition.class)
class GrpcNativeSecurityConfigurerAutoConfiguration {

	@Bean
	public GrpcSecurity grpcSecurity(ObjectPostProcessor<Object> objectPostProcessor,
			AuthenticationConfiguration authenticationConfiguration, ApplicationContext context) throws Exception {
		AuthenticationManagerBuilder authenticationManagerBuilder = authenticationConfiguration
			.authenticationManagerBuilder(objectPostProcessor, context);
		authenticationManagerBuilder
			.parentAuthenticationManager(authenticationConfiguration.getAuthenticationManager());
		return new GrpcSecurity(objectPostProcessor, authenticationManagerBuilder, context);
	}

}

@ConditionalOnBean(SecurityFilterChain.class)
@Conditional(GrpcServerFactoryAutoConfiguration.OnGrpcServletCondition.class)
@Configuration(proxyBeanMethods = false)
class GrpcServletSecurityConfigurerAutoConfiguration {

	@Bean
	@GlobalServerInterceptor
	public SecurityContextServerInterceptor securityContextInterceptor() {
		return new SecurityContextServerInterceptor();
	}

	@Bean
	@ConditionalOnMissingBean(GrpcServerExecutorProvider.class)
	public GrpcServerExecutorProvider grpcServerExecutorProvider() {
		return () -> new DelegatingSecurityContextExecutor(GrpcUtil.SHARED_CHANNEL_EXECUTOR.create());
	}

}
// CHECKSTYLE:ON
