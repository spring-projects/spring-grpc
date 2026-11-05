/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.ServerCall;

class SecurityContextHandlerListener<ReqT, RespT> extends SimpleForwardingServerCallListener<ReqT> {

	private SecurityContext securityContext;

	SecurityContextHandlerListener(ServerCall.Listener<ReqT> delegate, SecurityContext securityContext) {
		super(delegate);
		this.securityContext = securityContext;
	}

	@Override
	public void onMessage(ReqT message) {
		SecurityContextHolder.setContext(this.securityContext);
		try {
			super.onMessage(message);
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Override
	public void onHalfClose() {
		SecurityContextHolder.setContext(this.securityContext);
		try {
			super.onHalfClose();
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Override
	public void onReady() {
		SecurityContextHolder.setContext(this.securityContext);
		try {
			super.onReady();
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Override
	public void onCancel() {
		super.onCancel();
		SecurityContextHolder.clearContext();
	}

	@Override
	public void onComplete() {
		super.onComplete();
		SecurityContextHolder.clearContext();
	}

}
