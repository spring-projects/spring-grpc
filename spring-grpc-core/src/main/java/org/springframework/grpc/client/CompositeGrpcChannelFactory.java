/*
 * Copyright 2025-2025 the original author or authors.
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

package org.springframework.grpc.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

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
	public ManagedChannel createChannel(final String target, ChannelBuilderOptions options) {
		return this.channelFactories.stream()
			.filter((cf) -> cf.supports(target))
			.findFirst()
			.orElseThrow(
					() -> new IllegalStateException("No grpc channel factory found that supports target : " + target))
			.createChannel(target, options);
	}

}
