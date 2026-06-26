/*
 * Copyright 2023-present the original author or authors.
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
