/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import java.util.Objects;

import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import kotlin.coroutines.AbstractCoroutineContextElement;
import kotlin.coroutines.CoroutineContext;
import kotlinx.coroutines.ThreadContextElement;

/**
 * Holds a Spring Security {@link SecurityContext} in a Kotlin CoroutineContext.
 *
 * @author Dave Syer
 */
// To be replaced by official implementation when available from Spring Security
class SecurityContextElement extends AbstractCoroutineContextElement implements ThreadContextElement<SecurityContext> {

	/** The context key. */
	private static final Key Key = new Key();

	private final SecurityContext securityContext;

	SecurityContextElement(SecurityContext securityContext) {
		super(Key);
		this.securityContext = securityContext;
	}

	@Override
	public SecurityContext updateThreadContext(CoroutineContext coroutineContext) {
		Objects.requireNonNull(coroutineContext, "coroutineContext must not be null");
		SecurityContextHolder.setContext(this.securityContext);
		return this.securityContext;
	}

	@Override
	public void restoreThreadContext(CoroutineContext coroutineContext, SecurityContext securityContext) {
		Objects.requireNonNull(coroutineContext, "coroutineContext must not be null");
		SecurityContextHolder.clearContext();
	}

	public static final class Key implements CoroutineContext.Key<SecurityContextElement> {

	}

}
