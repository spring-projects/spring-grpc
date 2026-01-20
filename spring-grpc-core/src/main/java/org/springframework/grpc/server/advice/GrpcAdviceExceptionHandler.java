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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;

/**
 * Handles exceptions by delegating to {@link GrpcExceptionHandler @GrpcExceptionHandler}
 * methods in {@link GrpcAdvice @GrpcAdvice} beans.
 *
 * @author Oleksandr Shevchenko
 * @see GrpcAdvice
 * @see GrpcExceptionHandler
 */
public class GrpcAdviceExceptionHandler implements org.springframework.grpc.server.exception.GrpcExceptionHandler {

	private static final Log logger = LogFactory.getLog(GrpcAdviceExceptionHandler.class);

	private final GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver;

	/**
	 * Create a new instance.
	 * @param grpcExceptionHandlerMethodResolver the method resolver to use
	 */
	public GrpcAdviceExceptionHandler(GrpcExceptionHandlerMethodResolver grpcExceptionHandlerMethodResolver) {
		Assert.notNull(grpcExceptionHandlerMethodResolver, "grpcExceptionHandlerMethodResolver must not be null");
		this.grpcExceptionHandlerMethodResolver = grpcExceptionHandlerMethodResolver;
	}

	@Override
	public @Nullable StatusException handleException(Throwable exception) {
		try {
			Object mappedReturnType = handleThrownException(exception);
			if (mappedReturnType == null) {
				return null;
			}
			Status status = resolveStatus(mappedReturnType);
			Metadata metadata = resolveMetadata(mappedReturnType);
			return status.asException(metadata);
		}
		catch (Throwable errorWhileResolving) {
			if (errorWhileResolving != exception) {
				errorWhileResolving.addSuppressed(exception);
			}
			logger.error("Exception thrown during invocation of annotated @GrpcExceptionHandler method: ",
					errorWhileResolving);
			return Status.INTERNAL.withCause(errorWhileResolving)
				.withDescription("There was a server error trying to handle an exception")
				.asException();
		}
	}

	/**
	 * Resolve the gRPC status from the handler's return value.
	 * @param mappedReturnType the handler return value
	 * @return the resolved status
	 */
	protected Status resolveStatus(Object mappedReturnType) {
		if (mappedReturnType instanceof Status status) {
			return status;
		}
		else if (mappedReturnType instanceof Throwable throwable) {
			return Status.fromThrowable(throwable);
		}
		throw new IllegalStateException(
				String.format("Error for mapped return type [%s] inside @GrpcAdvice, it has to be of type: "
						+ "[Status, StatusException, StatusRuntimeException, Throwable]", mappedReturnType));
	}

	/**
	 * Resolve the metadata from the handler's return value.
	 * @param mappedReturnType the handler return value
	 * @return the resolved metadata, or empty metadata
	 */
	protected Metadata resolveMetadata(Object mappedReturnType) {
		Metadata result = null;
		if (mappedReturnType instanceof StatusException statusException) {
			result = statusException.getTrailers();
		}
		else if (mappedReturnType instanceof StatusRuntimeException statusRuntimeException) {
			result = statusRuntimeException.getTrailers();
		}
		return (result == null) ? new Metadata() : result;
	}

	/**
	 * Look up and invoke the handler method for the given exception.
	 * @param exception the exception to handle
	 * @return the handler result, or {@code null} if no handler is mapped
	 * @throws Throwable if the handler throws an exception
	 */
	@Nullable
	protected Object handleThrownException(Throwable exception) throws Throwable {
		if (logger.isDebugEnabled()) {
			logger.debug("Exception caught during gRPC execution: " + exception);
		}

		Class<? extends Throwable> exceptionClass = exception.getClass();
		boolean exceptionIsMapped = this.grpcExceptionHandlerMethodResolver.isMethodMappedForException(exceptionClass);
		if (!exceptionIsMapped) {
			return null;
		}

		Entry<@Nullable Object, @Nullable Method> methodWithInstance = this.grpcExceptionHandlerMethodResolver
			.resolveMethodWithInstance(exceptionClass);
		Method mappedMethod = methodWithInstance.getValue();
		Object instanceOfMappedMethod = methodWithInstance.getKey();

		if (mappedMethod == null || instanceOfMappedMethod == null) {
			return null;
		}

		Object[] instancedParams = determineInstancedParameters(mappedMethod, exception);
		return invokeMappedMethodSafely(mappedMethod, instanceOfMappedMethod, instancedParams);
	}

	private Object[] determineInstancedParameters(Method mappedMethod, Throwable exception) {
		Parameter[] parameters = mappedMethod.getParameters();
		Object[] instancedParams = new Object[parameters.length];

		for (int i = 0; i < parameters.length; i++) {
			Class<?> parameterClass = convertToClass(parameters[i]);
			if (parameterClass.isAssignableFrom(exception.getClass())) {
				instancedParams[i] = exception;
				break;
			}
		}
		return instancedParams;
	}

	private Class<?> convertToClass(Parameter parameter) {
		Type paramType = parameter.getParameterizedType();
		if (paramType instanceof Class<?> clazz) {
			return clazz;
		}
		throw new IllegalStateException("Parameter type of method has to be from Class, it was: " + paramType);
	}

	private Object invokeMappedMethodSafely(Method mappedMethod, Object instanceOfMappedMethod,
			Object[] instancedParams) throws Throwable {
		try {
			return mappedMethod.invoke(instanceOfMappedMethod, instancedParams);
		}
		catch (InvocationTargetException ex) {
			throw ex.getCause();
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException("Could not access @GrpcExceptionHandler method: " + mappedMethod, ex);
		}
	}

}
