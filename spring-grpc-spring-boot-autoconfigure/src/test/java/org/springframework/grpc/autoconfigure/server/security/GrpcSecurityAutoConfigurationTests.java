/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.grpc.autoconfigure.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.autoconfigure.server.GrpcServerAutoConfiguration;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.grpc.server.security.SecurityGrpcExceptionHandler;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Tests for {@link GrpcServerAutoConfiguration}.
 *
 * @author Chris Bono
 */
class GrpcSecurityAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		// NOTE: we use noop server lifecycle to avoid startup
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcSecurityAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock);
	}

	@Test
	void whenObjectPostProcessorNotOnClasspathAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withClassLoader(new FilteredClassLoader(ObjectPostProcessor.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcSecurityAutoConfiguration.class));
	}

	@Test
	void whenObjectPostProcessorPresentGrpcSecurityIsCreated() {
		new ApplicationContextRunner()
			.withInitializer(ConditionEvaluationReportLoggingListener.forLogLevel(LogLevel.INFO))
			.withConfiguration(AutoConfigurations.of(GrpcSecurityAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(GrpcSecurity.class);
			});
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcSecurityAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner().run((context) -> assertThat(context).hasSingleBean(GrpcSecurityAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcSecurityAutoConfiguration.class));
	}

	@Test
	void grpcSecurityAutoConfiguredAsExpected() {
		this.contextRunner().run((context) -> {
			assertThat(context).getBean(GrpcExceptionHandler.class).isInstanceOf(SecurityGrpcExceptionHandler.class);
			assertThat(context).getBean(AuthenticationProcessInterceptor.class).isNull();
		});
	}

	@EnableMethodSecurity
	@Configuration(proxyBeanMethods = false)
	static class ExtraConfiguration {

	}

}
