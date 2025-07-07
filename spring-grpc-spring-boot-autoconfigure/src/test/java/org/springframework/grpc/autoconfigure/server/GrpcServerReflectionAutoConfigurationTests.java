/*
 * Copyright 2023-2025 the original author or authors.
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
package org.springframework.grpc.autoconfigure.server;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;

import io.grpc.BindableService;

/**
 * Tests for {@link GrpcServerReflectionAutoConfiguration}.
 *
 * @author Haris Zujo
 * @author Chris Bono
 * @author Andrey Litvitski
 */
class GrpcServerReflectionAutoConfigurationTests {

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerReflectionAutoConfiguration.class))
			.withBean("noopServerLifecycle", GrpcServerLifecycle.class, Mockito::mock)
			.withBean(BindableService.class, Mockito::mock);
	}

	@Test
	void whenAutoConfigurationIsNotSkippedThenCreatesReflectionServiceBean() {
		this.contextRunner().run((context -> assertThat(context).hasBean("serverReflection")));
	}

	@Test
	void whenNoBindableServiceDefinedThenAutoConfigurationIsSkipped() {
		new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GrpcServerReflectionAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenReflectionEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenReflectionEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.reflection.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenReflectionEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.reflection.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertyNotSetThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetTrueThenAutoConfigurationIsNotSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=true")
			.run((context) -> assertThat(context).hasSingleBean(GrpcServerReflectionAutoConfiguration.class));
	}

	@Test
	void whenServerEnabledPropertySetFalseThenAutoConfigurationIsSkipped() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.server.enabled=false")
			.run((context) -> assertThat(context).doesNotHaveBean(GrpcServerReflectionAutoConfiguration.class));
	}

}
