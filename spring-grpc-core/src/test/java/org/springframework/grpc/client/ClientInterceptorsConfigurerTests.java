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

package org.springframework.grpc.client;

import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;

/**
 * Tests for {@link ClientInterceptorsConfigurer}.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 */
class ClientInterceptorsConfigurerTests {

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(ClientInterceptorsConfigurerConfig.class));
	}

	@Test
	void globalClientInterceptorsAreFoundInProperOrder() {
		this.contextRunner()
			.withUserConfiguration(GlobalClientInterceptorsConfig.class)
			.run((context) -> Assertions.assertThat(context)
				.getBean(ClientInterceptorsConfigurer.class)
				.extracting("globalInterceptors", InstanceOfAssertFactories.LIST)
				.containsExactly(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
						GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
	}

	private void customizeContextAndRunConfigurer(
			Function<ApplicationContextRunner, ApplicationContextRunner> contextCustomizer,
			List<ClientInterceptor> clientSpecificInterceptors, List<ClientInterceptor> expectedInterceptors) {
		ManagedChannelBuilder<?> builder = Mockito.mock();
		this.contextRunner().with(contextCustomizer).run((context) -> {
			var factory = Mockito.mock(GrpcChannelFactory.class);
			var configurer = context.getBean(ClientInterceptorsConfigurer.class);
			configurer.configureInterceptors(builder, clientSpecificInterceptors, true, factory);
			// NOTE: the interceptors are called in reverse order per builder contract
			var expectedInterceptorsReversed = new ArrayList<>(expectedInterceptors);
			Collections.reverse(expectedInterceptorsReversed);
			verify(builder).intercept(expectedInterceptorsReversed);
		});
	}

	@Nested
	class WithOnlyGlobalInterceptors {

		@Test
		void whenNoGlobalInterceptorsRegisteredThenBuilderGetsNoInterceptors() {
			customizeContextAndRunConfigurer(Function.identity(), List.of(), List.of());
		}

		@Test
		void whenGlobalInterceptorsRegisteredThenBuilderGetsGlobalInterceptors() {
			customizeContextAndRunConfigurer(
					(contextRunner) -> contextRunner.withUserConfiguration(GlobalClientInterceptorsConfig.class),
					List.of(), List.of(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
		}

	}

	@Nested
	class WithOnlyClientSpecificInterceptors {

		@Test
		void whenSingleInterceptorSpecifiedThenItIsUsed() {
			var clientSpecificInterceptors = List
				.<ClientInterceptor>of(ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			var expectedInterceptors = List.<ClientInterceptor>of(ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunConfigurer(
					(contextRunner) -> contextRunner.withUserConfiguration(ClientSpecificInterceptorsConfig.class),
					clientSpecificInterceptors, expectedInterceptors);
		}

		@Test
		void whenMultipleInterceptorsSpecifiedThenTheyAreUsed() {
			var clientSpecificInterceptors = List.of(ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			var expectedInterceptors = List.of(ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ClientSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunConfigurer(
					(contextRunner) -> contextRunner.withUserConfiguration(ClientSpecificInterceptorsConfig.class),
					clientSpecificInterceptors, expectedInterceptors);
		}

	}

	@Nested
	class WithClientSpecificCombinedWithGlobalInterceptors {

		@Test
		void whenBlendInterceptorsFalseThenGlobalInterceptorsAddedFirst() {
			ManagedChannelBuilder<?> builder = Mockito.mock();
			ClientInterceptorsConfigurerTests.this.contextRunner()
				.withUserConfiguration(GlobalClientInterceptorsConfig.class, ClientSpecificInterceptorsConfig.class)
				.run((context) -> {
					var factory = Mockito.mock(GrpcChannelFactory.class);
					var interceptorA = context.getBean("interceptorA", ClientInterceptor.class);
					var interceptorB = context.getBean("interceptorB", ClientInterceptor.class);
					var clientSpecificInterceptors = List.of(interceptorB, interceptorA);
					var expectedInterceptors = List.of(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO, interceptorB, interceptorA);
					var configurer = context.getBean(ClientInterceptorsConfigurer.class);
					configurer.configureInterceptors(builder, clientSpecificInterceptors, false, factory);
					// NOTE: the interceptors are called in reverse order per builder
					// contract
					var expectedInterceptorsReversed = new ArrayList<>(expectedInterceptors);
					Collections.reverse(expectedInterceptorsReversed);
					verify(builder).intercept(expectedInterceptorsReversed);
				});
		}

		@SuppressWarnings("unchecked")
		@Test
		void whenBlendInterceptorsTrueThenGlobalInterceptorsBlended() {
			ManagedChannelBuilder<?> builder = Mockito.mock();
			ClientInterceptorsConfigurerTests.this.contextRunner()
				.withUserConfiguration(GlobalClientInterceptorsConfig.class, ClientSpecificInterceptorsConfig.class)
				.run((context) -> {
					var factory = Mockito.mock(GrpcChannelFactory.class);
					var interceptorA = context.getBean("interceptorA", ClientInterceptor.class);
					var interceptorB = context.getBean("interceptorB", ClientInterceptor.class);
					var clientSpecificInterceptors = List.of(interceptorB, interceptorA);
					var expectedInterceptors = List.of(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							interceptorB, GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO, interceptorA);
					var configurer = context.getBean(ClientInterceptorsConfigurer.class);
					configurer.configureInterceptors(builder, clientSpecificInterceptors, true, factory);
					// NOTE: the interceptors are called in reverse order per builder
					// contract
					var expectedInterceptorsReversed = new ArrayList<>(expectedInterceptors);
					Collections.reverse(expectedInterceptorsReversed);
					verify(builder).intercept(expectedInterceptorsReversed);
				});
		}

	}

	@Nested
	class WithInterceptorFilters {

		@Test
		void whenFilterExcludesOneGlobalInterceptor_thenBuilderGetsOnlyAllowedOnes() {
			ManagedChannelBuilder<?> builder = Mockito.mock();
			ClientInterceptorsConfigurerTests.this.contextRunner()
				.withUserConfiguration(GlobalClientInterceptorsConfig.class)
				.withBean(ClientInterceptorFilter.class,
						() -> (interceptor, __) -> interceptor == GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR)
				.run(context -> {
					var factory = Mockito.mock(GrpcChannelFactory.class);
					var configurer = context.getBean(ClientInterceptorsConfigurer.class);
					configurer.configureInterceptors(builder, List.of(), true, factory);
					var expectedInterceptors = List.of(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR);
					verify(builder).intercept(expectedInterceptors);
				});
		}

		@Test
		void whenFilterIncludesAllGlobalInterceptors_thenBuilderGetsOnlyAllowedOnes() {
			ManagedChannelBuilder<?> builder = Mockito.mock();
			ClientInterceptorsConfigurerTests.this.contextRunner()
				.withUserConfiguration(GlobalClientInterceptorsConfig.class)
				.withBean(ClientInterceptorFilter.class, () -> (interceptor, __) -> true)
				.run(context -> {
					var factory = Mockito.mock(GrpcChannelFactory.class);
					var configurer = context.getBean(ClientInterceptorsConfigurer.class);
					configurer.configureInterceptors(builder, List.of(), true, factory);
					var expectedInterceptors = List.of(GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							GlobalClientInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO);
					// NOTE: the interceptors are called in reverse order per builder
					// contract
					var expectedInterceptorsReversed = new ArrayList<>(expectedInterceptors);
					Collections.reverse(expectedInterceptorsReversed);
					verify(builder).intercept(expectedInterceptorsReversed);
				});
		}

	}

	interface TestClientInterceptorA extends ClientInterceptor {

	}

	interface TestClientInterceptorB extends ClientInterceptor {

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientInterceptorsConfigurerConfig {

		@Bean
		ClientInterceptorsConfigurer clientInterceptorsConfigurer(ApplicationContext applicationContext) {
			return new ClientInterceptorsConfigurer(applicationContext);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GlobalClientInterceptorsConfig {

		static ClientInterceptor GLOBAL_INTERCEPTOR_FOO = Mockito.mock();

		static ClientInterceptor GLOBAL_INTERCEPTOR_IGNORED = Mockito.mock();

		static ClientInterceptor GLOBAL_INTERCEPTOR_BAR = Mockito.mock();

		@Bean
		@Order(200)
		@GlobalClientInterceptor
		ClientInterceptor globalInterceptorFoo() {
			return GLOBAL_INTERCEPTOR_FOO;
		}

		@Bean
		@Order(150)
		ClientInterceptor globalInterceptorIgnored() {
			return GLOBAL_INTERCEPTOR_IGNORED;
		}

		@Bean
		@Order(100)
		@GlobalClientInterceptor
		ClientInterceptor globalInterceptorBar() {
			return GLOBAL_INTERCEPTOR_BAR;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ClientSpecificInterceptorsConfig {

		static TestClientInterceptorB SVC_INTERCEPTOR_B = Mockito.mock();

		static TestClientInterceptorA SVC_INTERCEPTOR_A = Mockito.mock();

		@Bean
		@Order(150)
		TestClientInterceptorB interceptorB() {
			return SVC_INTERCEPTOR_B;
		}

		@Bean
		@Order(225)
		TestClientInterceptorA interceptorA() {
			return SVC_INTERCEPTOR_A;
		}

	}

}
