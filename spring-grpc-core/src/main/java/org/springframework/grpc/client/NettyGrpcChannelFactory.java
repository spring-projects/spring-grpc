/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
