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

package org.springframework.grpc.client;

import java.util.List;

import io.grpc.ChannelCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;

/**
 * {@link GrpcChannelFactory} that creates shaded Netty-based gRPC channels.
 *
 * @author Chris Bono
 */
public class ShadedNettyGrpcChannelFactory extends DefaultGrpcChannelFactory<NettyChannelBuilder> {

	/**
	 * Construct a channel factory instance.
	 * @param globalCustomizers the global customizers to apply to all created channels
	 * @param interceptorsConfigurer configures the client interceptors on the created
	 * channels
	 */
	public ShadedNettyGrpcChannelFactory(List<GrpcChannelBuilderCustomizer<NettyChannelBuilder>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(globalCustomizers, interceptorsConfigurer);
	}

	@Override
	protected NettyChannelBuilder newChannelBuilder(String path, ChannelCredentials credentials) {
		if (path.startsWith("unix:")) {
			path = path.substring(5);
			return NettyChannelBuilder.forAddress(new DomainSocketAddress(path))
				.channelType(EpollDomainSocketChannel.class)
				.eventLoopGroup(new EpollEventLoopGroup());
		}
		return NettyChannelBuilder.forTarget(path, credentials);
	}

}
