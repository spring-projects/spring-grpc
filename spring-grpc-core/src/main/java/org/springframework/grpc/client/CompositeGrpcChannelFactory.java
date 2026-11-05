/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;

/**
 * A composite {@link GrpcChannelFactory} that combines a list of channel factories.
 * <p>
 * The composite delegates channel creation to the first composed factory that supports
 * the given target string.
 *
 * @author Chris Bono
 */
public class CompositeGrpcChannelFactory implements GrpcChannelFactory {

	private List<GrpcChannelFactory> channelFactories = new ArrayList<>();

	/**
	 * Creates a new CompositeGrpcChannelFactory with the given factories.
	 * @param channelFactories the channel factories
	 */
	public CompositeGrpcChannelFactory(List<GrpcChannelFactory> channelFactories) {
		Assert.notEmpty(channelFactories, "composite channel factory requires at least one channel factory");
		this.channelFactories.addAll(channelFactories);
	}

	@Override
	public boolean supports(String target) {
		return this.channelFactories.stream().anyMatch((cf) -> cf.supports(target));
	}

	@Override
	public boolean supports(ClientInterceptor interceptor) {
		return this.channelFactories.stream().anyMatch((cf) -> cf.supports(interceptor));
	}

	@Override
	public ManagedChannel createChannel(final String target, ChannelBuilderOptions options) {
		return this.channelFactories.stream()
			.filter((cf) -> cf.supports(target))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No grpc channel factory found that supports target : " + target))
			.createChannel(target, options);
	}

}
