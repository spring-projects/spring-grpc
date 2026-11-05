/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
