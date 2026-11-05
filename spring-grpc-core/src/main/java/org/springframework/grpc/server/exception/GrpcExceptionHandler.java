/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.exception;

import org.jspecify.annotations.Nullable;

import io.grpc.StatusException;

/**
 * Defines an exception handler for handling exceptions that occur during gRPC server-side
 * processing. Implementations of this interface can be used to customize the error
 * handling behavior of a gRPC server.
 *
 * @author Dave Syer
 */
public interface GrpcExceptionHandler {

	/**
	 * Handle the given exception that occurred during gRPC server-side processing.
	 * @param exception the exception to handle
	 * @return the status to return to the client, or {@code null} if the exception cannot
	 * be classified
	 */
	@Nullable
	StatusException handleException(Throwable exception);

}
