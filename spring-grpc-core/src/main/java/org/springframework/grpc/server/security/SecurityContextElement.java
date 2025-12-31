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
