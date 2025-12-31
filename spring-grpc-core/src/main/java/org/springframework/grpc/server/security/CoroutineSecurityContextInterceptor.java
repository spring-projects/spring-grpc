/*
 * Copyright 2025-present the original author or authors.
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

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.kotlin.CoroutineContextServerInterceptor;
import kotlin.coroutines.CoroutineContext;

/**
 * A gRPC server interceptor that integrates Spring Security's {@link SecurityContext}
 * with Kotlin coroutines by adding an element to the coroutine context.
 *
 * @author Dave Syer
 */
public class CoroutineSecurityContextInterceptor extends CoroutineContextServerInterceptor {

	@Override
	public CoroutineContext coroutineContext(ServerCall<?, ?> call, Metadata metadata) {
		return new SecurityContextElement(SecurityContextHolder.getContext());
	}

}
