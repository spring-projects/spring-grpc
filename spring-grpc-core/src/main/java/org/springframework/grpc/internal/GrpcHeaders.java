/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.internal;

import io.grpc.Metadata;

/**
 * Common gRPC headers.
 * <p>
 * NOTE: Even though this class visibility is `public` it is intended for internal use
 * only and not recommended for direct use.
 *
 * @author Andrey Litvitski
 */
public final class GrpcHeaders {

	private GrpcHeaders() {
	}

	/**
	 * A constant key used for storing and retrieving the "Authorization" header from gRPC
	 * metadata. This key is used to handle authorization information in gRPC requests.
	 *
	 * <p>
	 * The key is defined with the name "Authorization" and uses the ASCII string
	 * marshaller for encoding and decoding the header value.
	 * </p>
	 */
	public static final Metadata.Key<String> AUTHORIZATION_KEY = Metadata.Key.of("Authorization",
			Metadata.ASCII_STRING_MARSHALLER);

}
