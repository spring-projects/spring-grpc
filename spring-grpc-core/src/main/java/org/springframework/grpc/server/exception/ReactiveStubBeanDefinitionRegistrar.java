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

import java.lang.reflect.Method;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.MethodReplacer;
import org.springframework.beans.factory.support.ReplaceOverride;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ReflectionUtils;

import io.grpc.BindableService;
import io.grpc.StatusException;

/**
 * A {@link BeanFactoryPostProcessor} and {@link MethodReplacer} that processes beans of
 * type {@link BindableService} to replace their {@code onErrorMap} method to a set of
 * {@link GrpcExceptionHandler} beans.
 *
 * @author Dave Syer
 */
public class ReactiveStubBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(ReactiveStubBeanFactoryPostProcessor.BEAN_NAME)) {
			return;
		}
		registry.registerBeanDefinition(ReactiveStubBeanFactoryPostProcessor.BEAN_NAME,
				BeanDefinitionBuilder.genericBeanDefinition(ReactiveStubBeanFactoryPostProcessor.class)
					.getBeanDefinition());
	}

	static class ReactiveStubBeanFactoryPostProcessor
			implements BeanFactoryPostProcessor, MethodReplacer, ApplicationContextAware {

		/**
		 * Bean name for this post processor in the application context.
		 */
		public static final String BEAN_NAME = ReactiveStubBeanFactoryPostProcessor.class.getName();

		private @Nullable CompositeGrpcExceptionHandler handler;

		private @Nullable ApplicationContext context;

		@Override
		public void setApplicationContext(ApplicationContext context) throws BeansException {
			this.context = context;
		}

		private ApplicationContext requireNonNullContext() {
			return Objects.requireNonNull(this.context, "context must not be null");
		}

		private Throwable onErrorMap(Throwable throwable) {
			if (this.handler == null) {
				GrpcExceptionHandler[] handlers = requireNonNullContext().getAutowireCapableBeanFactory()
					.getBeanProvider(GrpcExceptionHandler.class)
					.orderedStream()
					.toArray(GrpcExceptionHandler[]::new);
				this.handler = new CompositeGrpcExceptionHandler(handlers);
			}
			StatusException status = this.handler.handleException(throwable);
			return status != null ? status : throwable;
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) {
			if (requireNonNullContext().getBeanNamesForType(GrpcExceptionHandler.class).length == 0) {
				return;
			}
			for (String name : factory.getBeanNamesForType(BindableService.class)) {
				BeanDefinition service = factory.getBeanDefinition(name);
				Class<?> type = factory.getType(name);
				if (type != null) {
					Method method = ReflectionUtils.findMethod(type, "onErrorMap", Throwable.class);
					if (method != null && method.getDeclaringClass() != type
							&& service instanceof AbstractBeanDefinition root) {
						ReplaceOverride override = new ReplaceOverride("onErrorMap", BEAN_NAME);
						// You need this in an AOT build (but the interceptor still
						// isn't used at runtime with AOT
						// spring-projects/spring-framework#34642)
						override.addTypeIdentifier("Throwable");
						root.getMethodOverrides().addOverride(override);
					}
				}
			}
		}

		@Override
		public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
			return onErrorMap((Throwable) args[0]);
		}

	}

}
