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
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.EpollDomainSocketChannel;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * {@link GrpcChannelFactory} that creates Netty-based gRPC channels.
 *
 * @author Chris Bono
 * @author Andrey Litvitski
 */
public class NettyGrpcChannelFactory extends DefaultGrpcChannelFactory<NettyChannelBuilder> {

	/**
	 * Construct a channel factory instance.
	 * @param globalCustomizers the global customizers to apply to all created channels
	 * @param interceptorsConfigurer configures the client interceptors on the created
	 * channels
	 */
	public NettyGrpcChannelFactory(List<GrpcChannelBuilderCustomizer<NettyChannelBuilder>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(globalCustomizers, interceptorsConfigurer);
	}

	@Override
	protected NettyChannelBuilder newChannelBuilder(String target, ChannelCredentials credentials) {
		if (target.startsWith("unix:")) {
			target = target.substring(5);
			return NettyChannelBuilder.forAddress(new DomainSocketAddress(target))
				.channelType(EpollDomainSocketChannel.class)
				.eventLoopGroup(new MultiThreadIoEventLoopGroup(EpollIoHandler.newFactory()));
		}
		return NettyChannelBuilder.forTarget(target, credentials);
	}

}
