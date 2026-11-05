/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server;

import java.util.List;

import io.grpc.inprocess.InProcessServerBuilder;

/**
 * {@link GrpcServerFactory} that can be used to create an in-process gRPC server.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 */
public class InProcessGrpcServerFactory extends DefaultGrpcServerFactory<InProcessServerBuilder> {

	public InProcessGrpcServerFactory(String address,
			List<ServerBuilderCustomizer<InProcessServerBuilder>> serverBuilderCustomizers) {
		super(address, serverBuilderCustomizers, null, null, null);
	}

	@Override
	protected InProcessServerBuilder newServerBuilder() {
		return InProcessServerBuilder.forName(address());
	}

}
