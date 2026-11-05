/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
