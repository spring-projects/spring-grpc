/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.service;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * Encapsulates enough information to construct an actual {@link ServerServiceDefinition}.
 *
 * @param service the bindable service
 * @param serviceInfo optional additional information about the service (e.g.
 * interceptors)
 * @author Chris Bono
 */
public record GrpcServiceSpec(BindableService service, @Nullable GrpcServiceInfo serviceInfo) {
	public GrpcServiceSpec {
		Assert.notNull(service, "service must not be null");
	}

}
