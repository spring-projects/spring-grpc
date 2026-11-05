/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import org.jspecify.annotations.Nullable;

import org.springframework.security.core.Authentication;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public interface GrpcAuthenticationExtractor {

	@Nullable
	Authentication extract(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method);

}
