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
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.ExceptionDepthComparator;
import org.springframework.util.Assert;

/**
 * Maps exception types to {@link GrpcExceptionHandler @GrpcExceptionHandler} methods.
 *
 * @author Oleksandr Shevchenko
 * @see GrpcAdvice
 * @see GrpcExceptionHandler
 */
public class GrpcExceptionHandlerMethodResolver implements InitializingBean {

	private final Map<Class<? extends Throwable>, Method> mappedMethods = new HashMap<>(16);

	private final GrpcAdviceDiscoverer grpcAdviceDiscoverer;

	@SuppressWarnings("unchecked")
	private Class<? extends Throwable>[] annotatedExceptions = new Class[0];

	/**
	 * Create a new instance.
	 * @param grpcAdviceDiscoverer the advice discoverer to use
	 */
	public GrpcExceptionHandlerMethodResolver(GrpcAdviceDiscoverer grpcAdviceDiscoverer) {
		Assert.notNull(grpcAdviceDiscoverer, "grpcAdviceDiscoverer must not be null");
		this.grpcAdviceDiscoverer = grpcAdviceDiscoverer;
	}

	@Override
	public void afterPropertiesSet() {
		this.grpcAdviceDiscoverer.getAnnotatedMethods().forEach(this::extractAndMapExceptionToMethod);
	}

	@SuppressWarnings("unchecked")
	private void extractAndMapExceptionToMethod(Method method) {
		GrpcExceptionHandler annotation = method.getDeclaredAnnotation(GrpcExceptionHandler.class);
		Assert.notNull(annotation, "@GrpcExceptionHandler annotation not found.");
		this.annotatedExceptions = annotation.value();

		checkForPresentExceptionToMap(method);
		Set<Class<? extends Throwable>> exceptionsToMap = extractExceptions(method.getParameterTypes());
		exceptionsToMap.forEach(exceptionType -> addExceptionMapping(exceptionType, method));
	}

	private void checkForPresentExceptionToMap(Method method) {
		if (method.getParameterTypes().length == 0 && this.annotatedExceptions.length == 0) {
			throw new IllegalStateException(String
				.format("@GrpcExceptionHandler annotated method [%s] has no mapped exception!", method.getName()));
		}
	}

	private Set<Class<? extends Throwable>> extractExceptions(Class<?>[] methodParamTypes) {
		Set<Class<? extends Throwable>> exceptionsToBeMapped = new HashSet<>();
		for (Class<? extends Throwable> annoClass : this.annotatedExceptions) {
			if (methodParamTypes.length > 0) {
				validateAppropriateParentException(annoClass, methodParamTypes);
			}
			exceptionsToBeMapped.add(annoClass);
		}

		addMappingInCaseAnnotationIsEmpty(methodParamTypes, exceptionsToBeMapped);
		return exceptionsToBeMapped;
	}

	private void validateAppropriateParentException(Class<? extends Throwable> annoClass, Class<?>[] methodParamTypes) {
		boolean paramTypeIsNotSuperclass = Arrays.stream(methodParamTypes)
			.noneMatch(param -> param.isAssignableFrom(annoClass));
		if (paramTypeIsNotSuperclass) {
			throw new IllegalStateException(String.format(
					"No listed parameter argument [%s] is equal or superclass "
							+ "of annotated @GrpcExceptionHandler method declared exception [%s].",
					Arrays.toString(methodParamTypes), annoClass));
		}
	}

	@SuppressWarnings("unchecked")
	private void addMappingInCaseAnnotationIsEmpty(Class<?>[] methodParamTypes,
			Set<Class<? extends Throwable>> exceptionsToBeMapped) {
		Arrays.stream(methodParamTypes)
			.filter(param -> exceptionsToBeMapped.isEmpty())
			.filter(Throwable.class::isAssignableFrom)
			.map(clazz -> (Class<? extends Throwable>) clazz)
			.forEach(exceptionsToBeMapped::add);
	}

	private void addExceptionMapping(Class<? extends Throwable> exceptionType, Method method) {
		Method oldMethod = this.mappedMethods.put(exceptionType, method);
		if (oldMethod != null && !oldMethod.equals(method)) {
			throw new IllegalStateException("Ambiguous @GrpcExceptionHandler method mapped for [" + exceptionType
					+ "]: {" + oldMethod + ", " + method + "}");
		}
	}

	/**
	 * Resolve the handler method and bean instance for the given exception type.
	 * @param <E> the exception type
	 * @param exceptionType the exception type to resolve
	 * @return entry with bean and method, or nulls if no mapping exists
	 */
	public <E extends Throwable> Map.Entry<@Nullable Object, @Nullable Method> resolveMethodWithInstance(
			Class<E> exceptionType) {
		Method value = extractExtendedThrowable(exceptionType);
		if (value == null) {
			return new SimpleImmutableEntry<>(null, null);
		}

		Class<?> methodClass = value.getDeclaringClass();
		Object key = this.grpcAdviceDiscoverer.getAnnotatedBeans()
			.values()
			.stream()
			.filter(obj -> methodClass.isAssignableFrom(obj.getClass()))
			.findFirst()
			.orElse(null);
		return new SimpleImmutableEntry<>(key, value);
	}

	/**
	 * Check if a handler method exists for the given exception type.
	 * @param <E> the exception type
	 * @param exception the exception type to check
	 * @return {@code true} if a handler is mapped
	 */
	public <E extends Throwable> boolean isMethodMappedForException(Class<E> exception) {
		return extractExtendedThrowable(exception) != null;
	}

	@Nullable
	private <E extends Throwable> Method extractExtendedThrowable(Class<E> exceptionType) {
		return this.mappedMethods.keySet()
			.stream()
			.filter(ex -> ex.isAssignableFrom(exceptionType))
			.min(new ExceptionDepthComparator(exceptionType))
			.map(this.mappedMethods::get)
			.orElse(null);
	}

}
