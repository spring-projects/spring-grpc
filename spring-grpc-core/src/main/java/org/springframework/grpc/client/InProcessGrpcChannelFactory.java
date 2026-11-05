/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.List;

import io.grpc.ChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * {@link GrpcChannelFactory} that creates in-process gRPC channels.
 *
 * @author Chris Bono
 */
public class InProcessGrpcChannelFactory extends DefaultGrpcChannelFactory<InProcessChannelBuilder> {

	/**
	 * Construct an in-process channel factory instance and sets the
	 * {@link #setVirtualTargets virtualTargets} to the identity function so that the
	 * exact passed in target string is used as the target of the channel factory.
	 * @param globalCustomizers the global customizers to apply to all created channels
	 * @param interceptorsConfigurer configures the client interceptors on the created
	 * channels
	 */
	public InProcessGrpcChannelFactory(List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(globalCustomizers, interceptorsConfigurer);
		setVirtualTargets((p) -> p);
	}

	/**
	 * {@inheritDoc}
	 * @param target the target string
	 * @return true if the target begins with 'in-process:'
	 */
	@Override
	public boolean supports(String target) {
		if (target.startsWith("in-process:")) {
			return true;
		}
		var targetUri = this.targets.getTarget(target);
		return targetUri.startsWith("in-process:");
	}

	@Override
	protected InProcessChannelBuilder newChannelBuilder(String target, ChannelCredentials creds) {
		return InProcessChannelBuilder.forName(target.substring(11));
	}

}
