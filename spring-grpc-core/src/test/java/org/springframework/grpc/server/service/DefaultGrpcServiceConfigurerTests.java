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

package org.springframework.grpc.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.lang.Nullable;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link DefaultGrpcServiceConfigurer}.
 *
 * @author Chris Bono
 */
class DefaultGrpcServiceConfigurerTests {

	private AnnotationConfigApplicationContext appContextForConfigurations(List<Class<?>> configClasses) {
		return this.appContextForConfigurations(configClasses, null);
	}

	private AnnotationConfigApplicationContext appContextForConfigurations(List<Class<?>> configClasses,
			@Nullable Consumer<AnnotationConfigApplicationContext> contextCustomizer) {
		List<Class<?>> allConfigClasses = new ArrayList();
		allConfigClasses.add(ServiceConfigurerConfig.class);
		allConfigClasses.addAll(configClasses);
		var context = new AnnotationConfigApplicationContext();
		context.register(allConfigClasses.toArray(new Class<?>[0]));
		context.registerBean("noopServerLifecycle", GrpcServerLifecycle.class, () -> mock());
		if (contextCustomizer != null) {
			contextCustomizer.accept(context);
		}
		context.refresh();
		return context;
	}

	@Test
	void globalServerInterceptorsAreFoundInProperOrder() {
		var context = this.appContextForConfigurations(List.of(GlobalServerInterceptorsConfig.class));
		assertThat(context.getBean(DefaultGrpcServiceConfigurer.class))
			.extracting("globalInterceptors", InstanceOfAssertFactories.LIST)
			.containsExactly(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO);
	}

	private void customizeContextAndRunServiceConfigurerWithServiceInfo(List<Class<?>> configClasses,
			GrpcServiceInfo serviceInfo, List<ServerInterceptor> expectedInterceptors) {
		this.doCustomizeContextAndRunServiceConfigurerWithServiceInfo(configClasses, serviceInfo, expectedInterceptors,
				null);
	}

	private void customizeContextAndRunServiceConfigurerWithServiceInfoExpectingException(List<Class<?>> configClasses,
			GrpcServiceInfo serviceInfo, Class<? extends Throwable> expectedExceptionType) {
		this.doCustomizeContextAndRunServiceConfigurerWithServiceInfo(configClasses, serviceInfo, null,
				expectedExceptionType);
	}

	private void doCustomizeContextAndRunServiceConfigurerWithServiceInfo(List<Class<?>> configClasses,
			GrpcServiceInfo serviceInfo, @Nullable List<ServerInterceptor> expectedInterceptors,
			@Nullable Class<? extends Throwable> expectedExceptionType) {
		// It gets difficult to verify interceptors are added properly to mocked
		// services.
		// To make it easier, we just static mock ServerInterceptors.interceptForward to
		// echo back the service def. This way we can verify the interceptors were
		// passed
		// in the proper order as we rely on ServerInterceptors.interceptForward being
		// well tested in grpc-java.
		try (MockedStatic<ServerInterceptors> serverInterceptorsMocked = Mockito.mockStatic(ServerInterceptors.class)) {
			serverInterceptorsMocked
				.when(() -> ServerInterceptors.interceptForward(any(ServerServiceDefinition.class), anyList()))
				.thenAnswer((Answer<ServerServiceDefinition>) invocation -> invocation.getArgument(0));
			BindableService service = mock();
			ServerServiceDefinition serviceDef = mock();
			when(service.bindService()).thenReturn(serviceDef);
			var context = this.appContextForConfigurations(configClasses,
					(appContext) -> appContext.registerBean("service", BindableService.class, () -> service));
			DefaultGrpcServiceConfigurer configurer = context.getBean(DefaultGrpcServiceConfigurer.class);
			if (expectedExceptionType != null) {
				assertThatThrownBy(() -> configurer.configure(new GrpcServiceSpec(service, serviceInfo), null))
					.isInstanceOf(expectedExceptionType);
				serverInterceptorsMocked.verifyNoInteractions();
			}
			else {
				configurer.configure(new GrpcServiceSpec(service, serviceInfo), null);
				serverInterceptorsMocked
					.verify(() -> ServerInterceptors.interceptForward(serviceDef, expectedInterceptors));
			}
		}
	}

	@Test
	void whenNoServiceSpecThenThrowsException() {
		var context = this.appContextForConfigurations(List.of());
		var configurer = context.getBean(DefaultGrpcServiceConfigurer.class);
		assertThatIllegalArgumentException().isThrownBy(() -> configurer.configure(null, null))
			.withMessage("serviceSpec must not be null");
	}

	@Nested
	class WithNoServiceInfoSpecified {

		@Test
		void whenNoGlobalInterceptorsRegisteredThenServiceGetsNoInterceptors() {
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(), null, List.of());
		}

		@Test
		void whenGlobalInterceptorsRegisteredThenServiceGetsGlobalInterceptors() {
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(GlobalServerInterceptorsConfig.class), null,
					List.of(GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
							GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO));
		}

	}

	@Nested
	class WithServiceInfoWithSingleInterceptor {

		@Test
		void whenSingleBeanOfInterceptorTypeRegisteredThenItIsUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

		@Test
		void whenMultipleBeansOfInterceptorTypeRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(ServerInterceptor.class));
			customizeContextAndRunServiceConfigurerWithServiceInfoExpectingException(
					List.of(ServiceSpecificInterceptorsConfig.class), serviceInfo,
					NoUniqueBeanDefinitionException.class);
		}

		@Test
		void whenNoBeanOfInterceptorTypeRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptors(List.of(ServerInterceptor.class));
			customizeContextAndRunServiceConfigurerWithServiceInfoExpectingException(List.of(), serviceInfo,
					NoSuchBeanDefinitionException.class);
		}

	}

	@Nested
	class WithServiceInfoWithMultipleInterceptors {

		@Test
		void whenSingleBeanOfEachInterceptorTypeRegisteredThenTheyAreUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo
				.withInterceptors(List.of(TestServerInterceptorB.class, TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoWithSingleInterceptorName {

		@Test
		void whenSingleBeanWithInterceptorNameRegisteredThenItIsUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptorB"));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B);
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

		@Test
		void whenNoBeanWithInterceptorNameRegisteredThenThrowsException() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptor1"));
			customizeContextAndRunServiceConfigurerWithServiceInfoExpectingException(List.of(), serviceInfo,
					NoSuchBeanDefinitionException.class);
		}

	}

	@Nested
	class WithServiceInfoWithMultipleInterceptorNames {

		@Test
		void whenSingleBeanWithEachInterceptorNameRegisteredThenTheyAreUsed() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo.withInterceptorNames(List.of("interceptorB", "interceptorA"));
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoWithInterceptorAndInterceptorName {

		@SuppressWarnings("unchecked")
		@Test
		void whenSingleBeanOfEachAvailableThenTheyAreBothUsed() {
			GrpcServiceInfo serviceInfo = new GrpcServiceInfo(new Class[] { TestServerInterceptorB.class },
					new String[] { "interceptorA" }, false);
			List<ServerInterceptor> expectedInterceptors = List.of(ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(List.of(ServiceSpecificInterceptorsConfig.class),
					serviceInfo, expectedInterceptors);
		}

	}

	@Nested
	class WithServiceInfoCombinedWithGlobalInterceptors {

		@Test
		void whenBlendInterceptorsFalseThenGlobalInterceptorsAddedFirst() {
			GrpcServiceInfo serviceInfo = GrpcServiceInfo
				.withInterceptors(List.of(TestServerInterceptorB.class, TestServerInterceptorA.class));
			List<ServerInterceptor> expectedInterceptors = List.of(
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					List.of(GlobalServerInterceptorsConfig.class, ServiceSpecificInterceptorsConfig.class), serviceInfo,
					expectedInterceptors);
		}

		@SuppressWarnings("unchecked")
		@Test
		void whenBlendInterceptorsTrueThenGlobalInterceptorsBlended() {
			GrpcServiceInfo serviceInfo = new GrpcServiceInfo(
					new Class[] { TestServerInterceptorB.class, TestServerInterceptorA.class }, new String[0], true);
			List<ServerInterceptor> expectedInterceptors = List.of(
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_BAR,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_B,
					GlobalServerInterceptorsConfig.GLOBAL_INTERCEPTOR_FOO,
					ServiceSpecificInterceptorsConfig.SVC_INTERCEPTOR_A);
			customizeContextAndRunServiceConfigurerWithServiceInfo(
					List.of(GlobalServerInterceptorsConfig.class, ServiceSpecificInterceptorsConfig.class), serviceInfo,
					expectedInterceptors);
		}

	}

	interface TestServerInterceptorA extends ServerInterceptor {

	}

	interface TestServerInterceptorB extends ServerInterceptor {

	}

	@Configuration(proxyBeanMethods = false)
	static class ServiceConfigurerConfig {

		@Bean
		GrpcServiceConfigurer grpcServiceConfigurer(ApplicationContext applicationContext) {
			return new DefaultGrpcServiceConfigurer(applicationContext);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class GlobalServerInterceptorsConfig {

		static ServerInterceptor GLOBAL_INTERCEPTOR_FOO = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_IGNORED = mock();

		static ServerInterceptor GLOBAL_INTERCEPTOR_BAR = mock();

		@Bean
		@Order(200)
		@GlobalServerInterceptor
		ServerInterceptor globalInterceptorFoo() {
			return GLOBAL_INTERCEPTOR_FOO;
		}

		@Bean
		@Order(150)
		ServerInterceptor globalInterceptorIgnored() {
			return GLOBAL_INTERCEPTOR_IGNORED;
		}

		@Bean
		@Order(100)
		@GlobalServerInterceptor
		ServerInterceptor globalInterceptorBar() {
			return GLOBAL_INTERCEPTOR_BAR;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ServiceSpecificInterceptorsConfig {

		static TestServerInterceptorB SVC_INTERCEPTOR_B = mock();

		static TestServerInterceptorA SVC_INTERCEPTOR_A = mock();

		@Bean
		@Order(150)
		TestServerInterceptorB interceptorB() {
			return SVC_INTERCEPTOR_B;
		}

		@Bean
		@Order(225)
		TestServerInterceptorA interceptorA() {
			return SVC_INTERCEPTOR_A;
		}

	}

}
