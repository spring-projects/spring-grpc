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

package org.springframework.grpc.server.advice;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils.MethodFilter;

/**
 * Discovers {@link GrpcAdvice @GrpcAdvice} beans and
 * {@link GrpcExceptionHandler @GrpcExceptionHandler} methods.
 *
 * @author Oleksandr Shevchenko
 */
public class GrpcAdviceDiscoverer implements InitializingBean {

	private static final Log logger = LogFactory.getLog(GrpcAdviceDiscoverer.class);

	/**
	 * A filter for selecting {@code @GrpcExceptionHandler} methods.
	 */
	public static final MethodFilter EXCEPTION_HANDLER_METHODS = method -> AnnotatedElementUtils.hasAnnotation(method,
			GrpcExceptionHandler.class);

	private final ApplicationContext applicationContext;

	private @Nullable Map<String, Object> annotatedBeans;

	private @Nullable Set<Method> annotatedMethods;

	public GrpcAdviceDiscoverer(ApplicationContext applicationContext) {
		Assert.notNull(applicationContext, "applicationContext must not be null");
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(GrpcAdvice.class);
		beans.forEach((key, value) -> {
			if (logger.isDebugEnabled()) {
				logger.debug("Found gRPC advice: " + key + ", class: " + value.getClass().getName());
			}
		});
		this.annotatedBeans = beans;
		this.annotatedMethods = findAnnotatedMethods(beans);
	}

	private Set<Method> findAnnotatedMethods(Map<String, Object> beans) {
		return beans.values()
			.stream()
			.map(Object::getClass)
			.map(this::findAnnotatedMethods)
			.flatMap(Collection::stream)
			.collect(Collectors.toSet());
	}

	private Set<Method> findAnnotatedMethods(Class<?> clazz) {
		return MethodIntrospector.selectMethods(clazz, EXCEPTION_HANDLER_METHODS);
	}

	/**
	 * Return the discovered {@link GrpcAdvice @GrpcAdvice} beans.
	 * @return the annotated beans
	 */
	public Map<String, Object> getAnnotatedBeans() {
		Assert.state(this.annotatedBeans != null, "@GrpcAdvice annotation scanning failed.");
		return this.annotatedBeans;
	}

	/**
	 * Return the discovered {@link GrpcExceptionHandler @GrpcExceptionHandler} methods.
	 * @return the annotated methods
	 */
	public Set<Method> getAnnotatedMethods() {
		Assert.state(this.annotatedMethods != null, "@GrpcExceptionHandler annotation scanning failed.");
		return this.annotatedMethods;
	}

}
