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

package org.springframework.grpc.server.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.Ordered;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import io.grpc.Status;
import io.grpc.StatusException;

public class SecurityGrpcExceptionHandler implements GrpcExceptionHandler, Ordered {

	private static final Log logger = LogFactory.getLog(SecurityGrpcExceptionHandler.class);

	private final int order;

	public SecurityGrpcExceptionHandler(int order) {
		this.order = order;
	}

	@Override
	public @Nullable StatusException handleException(Throwable exception) {
		if (exception instanceof AuthenticationException) {
			if (logger.isDebugEnabled()) {
				logger.error("Failed to authenticate", exception);
			}
			return Status.UNAUTHENTICATED.withDescription(exception.getMessage()).asException();
		}
		if (exception instanceof AccessDeniedException) {
			if (logger.isDebugEnabled()) {
				logger.error("Failed to authorize", exception);
			}
			return Status.PERMISSION_DENIED.withDescription(exception.getMessage()).asException();
		}
		return null;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
