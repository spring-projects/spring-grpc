/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.exception;

import org.jspecify.annotations.Nullable;

import io.grpc.StatusException;

public class CompositeGrpcExceptionHandler implements GrpcExceptionHandler {

	private final GrpcExceptionHandler[] exceptionHandlers;

	public CompositeGrpcExceptionHandler(GrpcExceptionHandler... exceptionHandlers) {
		this.exceptionHandlers = exceptionHandlers;
	}

	@Override
	public @Nullable StatusException handleException(Throwable exception) {
		for (GrpcExceptionHandler exceptionHandler : this.exceptionHandlers) {
			StatusException status = exceptionHandler.handleException(exception);
			if (status != null) {
				return status;
			}
		}
		return null;
	}

}
