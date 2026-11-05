/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.security;

import io.grpc.Attributes;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public record CallContext(Metadata headers, Attributes attributes, MethodDescriptor<?, ?> method) {

	@Override
	public final String toString() {
		return "CallContext:[" + this.method.getFullMethodName() + "]";
	}
}
