/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
