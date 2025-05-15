/*
 * Copyright 2024-2024 the original author or authors.
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

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.Status;
import io.grpc.StatusException;

public class GrpcExceptionHandledServerCall<ReqT, RespT>
		extends ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT> {

	private final GrpcExceptionHandler exceptionHandler;

	protected GrpcExceptionHandledServerCall(ServerCall<ReqT, RespT> delegate, GrpcExceptionHandler handler) {
		super(delegate);
		this.exceptionHandler = handler;
	}

	@Override
	public void close(Status status, Metadata trailers) {
		if (status.getCode() == Status.Code.UNKNOWN && status.getCause() != null) {
			final Throwable cause = status.getCause();
			final StatusException statusException = this.exceptionHandler.handleException(cause);
			super.close(statusException.getStatus(), trailers);
		}
		else {
			super.close(status, trailers);
		}
	}

}
