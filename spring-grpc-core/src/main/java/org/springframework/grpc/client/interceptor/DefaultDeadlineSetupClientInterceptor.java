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
		this.defaultDeadline = requireNonNull(defaultDeadline, "defaultDeadline");
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
