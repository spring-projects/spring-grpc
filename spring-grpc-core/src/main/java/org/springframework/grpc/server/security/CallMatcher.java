/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

interface CallMatcher {

	CallMatcher ALL = (context) -> true;

	boolean matches(CallContext context);

}
