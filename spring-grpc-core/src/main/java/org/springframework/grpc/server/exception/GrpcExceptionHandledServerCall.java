/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
			StatusException statusException = this.exceptionHandler.handleException(status.getCause());
			if (statusException != null) {
				Metadata statusExceptionTrailers = statusException.getTrailers();
				if (statusExceptionTrailers != null) {
					trailers.merge(statusExceptionTrailers);
				}
				status = statusException.getStatus();
			}
			super.close(status, trailers);
		}
		else {
			super.close(status, trailers);
		}
	}

}
