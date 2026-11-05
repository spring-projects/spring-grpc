/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client.interceptor.security;

import java.util.function.Supplier;

/*
 * Supplies a token for use in a gRPC call. This is used by the TokenInterceptor to add a token to the call metadata.
 */
public interface TokenSupplier extends Supplier<String> {

	@Override
	default String get() {
		return token();
	}

	String token();

}
