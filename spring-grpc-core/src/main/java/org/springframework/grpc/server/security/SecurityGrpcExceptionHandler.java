/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import io.grpc.Status;
import io.grpc.StatusException;

/**
 * Exception handler for server-side exceptions.
 *
 * @author Dave Syer
 */
public class SecurityGrpcExceptionHandler implements GrpcExceptionHandler {

	private static final Log logger = LogFactory.getLog(SecurityGrpcExceptionHandler.class);

	@Override
	public @Nullable StatusException handleException(Throwable exception) {
		if (exception instanceof AuthenticationException) {
			if (logger.isDebugEnabled()) {
				logger.error("Failed to authenticate", exception);
			}
			return Status.UNAUTHENTICATED.withDescription("Authentication failed").asException();
		}
		if (exception instanceof AccessDeniedException) {
			if (logger.isDebugEnabled()) {
				logger.error("Failed to authorize", exception);
			}
			return Status.PERMISSION_DENIED.withDescription("Access denied").asException();
		}
		return null;
	}

}
