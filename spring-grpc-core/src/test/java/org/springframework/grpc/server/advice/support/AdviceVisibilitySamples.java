/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
