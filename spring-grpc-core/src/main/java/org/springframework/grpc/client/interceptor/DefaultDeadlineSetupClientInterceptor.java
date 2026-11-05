/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client.interceptor;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;

/**
 * A client interceptor configuring the default deadline for each call.
 *
 * @author Sergei Batsura (batsura.sa@gmail.com)
 */
public class DefaultDeadlineSetupClientInterceptor implements ClientInterceptor {

	private final Duration defaultDeadline;

	public DefaultDeadlineSetupClientInterceptor(Duration defaultDeadline) {
		this.defaultDeadline = requireNonNull(defaultDeadline, "defaultDeadline must not be null");
	}

	@Override
	public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
			final CallOptions callOptions, final Channel next) {

		if (callOptions.getDeadline() == null) {
			return next.newCall(method,
					callOptions.withDeadlineAfter(this.defaultDeadline.toMillis(), TimeUnit.MILLISECONDS));
		}
		else {
			return next.newCall(method, callOptions);
		}
	}

}
