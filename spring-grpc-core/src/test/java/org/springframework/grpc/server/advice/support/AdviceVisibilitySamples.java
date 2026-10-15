/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.grpc.server.advice.support;

import org.springframework.grpc.server.advice.GrpcAdvice;
import org.springframework.grpc.server.advice.GrpcExceptionHandler;

import io.grpc.Status;

/**
 * Advice fixtures in a different package from the exception handler implementation.
 *
 * @author Jialin Chen
 */
public final class AdviceVisibilitySamples {

	private AdviceVisibilitySamples() {
	}

	public static Class<?> packagePrivateAdviceType() {
		return PackagePrivateAdvice.class;
	}

	@GrpcAdvice
	public static class PublicAdvice {

		@GrpcExceptionHandler
		public Status handleIllegalArgument(IllegalArgumentException exception) {
			return Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
		}

	}

	@GrpcAdvice
	static class PackagePrivateAdvice {

		@GrpcExceptionHandler
		public Status handleIllegalArgument(IllegalArgumentException exception) {
			return Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
		}

	}

}
