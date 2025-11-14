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

package org.springframework.grpc.server.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.type.AnnotationMetadata;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

public class ReactiveStubBeanDefinitionRegistrarTests {

	private static final String BEAN_NAME = ReactiveStubBeanDefinitionRegistrar.ReactiveStubBeanFactoryPostProcessor.BEAN_NAME;

	private StaticApplicationContext registry = new StaticApplicationContext();

	private ReactiveStubBeanDefinitionRegistrar registrar = new ReactiveStubBeanDefinitionRegistrar();

	private AnnotationMetadata metadata = null;

	private ReactiveStubBeanDefinitionRegistrar.ReactiveStubBeanFactoryPostProcessor processor;

	@BeforeEach
	void setup() {
		registry.registerSingleton("exceptionHandler", GrpcExceptionHandler.class);
		registrar.registerBeanDefinitions(metadata, registry);
		processor = (ReactiveStubBeanDefinitionRegistrar.ReactiveStubBeanFactoryPostProcessor) registry
			.getBean(BEAN_NAME);
		processor.setApplicationContext(registry);
	}

	@Test
	void defaultDoNothing() {
		assertThat(registry.containsBeanDefinition(BEAN_NAME)).isTrue();
	}

	@Test
	void postProcessNonReactiveBean() {
		registry.registerBean("service", MyService.class);
		processor.postProcessBeanFactory(registry.getDefaultListableBeanFactory());
		AbstractBeanDefinition bean = (AbstractBeanDefinition) registry.getBeanDefinition("service");
		assertThat(bean.hasMethodOverrides()).isFalse();
	}

	@Test
	void postProcessReactiveBean() {
		registry.registerBean("service", MyReactiveService.class);
		processor.postProcessBeanFactory(registry.getDefaultListableBeanFactory());
		AbstractBeanDefinition bean = (AbstractBeanDefinition) registry.getBeanDefinition("service");
		assertThat(bean.hasMethodOverrides()).isTrue();
	}

	@Test
	void postProcessReactiveBeanWithOnErrorMap() {
		registry.registerBean("service", MyReactiveStub.class);
		processor.postProcessBeanFactory(registry.getDefaultListableBeanFactory());
		AbstractBeanDefinition bean = (AbstractBeanDefinition) registry.getBeanDefinition("service");
		assertThat(bean.hasMethodOverrides()).isFalse();
	}

	static class MyService implements BindableService {

		@Override
		public ServerServiceDefinition bindService() {
			return null;
		}

	}

	static class MyReactiveService extends MyReactiveStub {

	}

	static class MyReactiveStub implements BindableService {

		@Override
		public ServerServiceDefinition bindService() {
			return null;
		}

		protected Throwable onErrorMap(Throwable throwable) {
			return throwable;
		}

	}

}
