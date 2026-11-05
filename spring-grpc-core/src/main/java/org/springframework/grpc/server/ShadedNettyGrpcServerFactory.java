/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.jspecify.annotations.Nullable;

import org.springframework.grpc.internal.GrpcUtils;

import io.grpc.TlsServerCredentials.ClientAuth;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollEventLoopGroup;
import io.grpc.netty.shaded.io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;

/**
 * {@link GrpcServerFactory} that can be used to create a shaded Netty-based gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 */
public class ShadedNettyGrpcServerFactory extends DefaultGrpcServerFactory<NettyServerBuilder> {

	public ShadedNettyGrpcServerFactory(String address,
			List<ServerBuilderCustomizer<NettyServerBuilder>> serverBuilderCustomizers,
			@Nullable KeyManagerFactory keyManager, @Nullable TrustManagerFactory trustManager,
			@Nullable ClientAuth clientAuth) {
		super(address, serverBuilderCustomizers, keyManager, trustManager, clientAuth);
	}

	@Override
	protected NettyServerBuilder newServerBuilder() {
		String address = address();
		if (address.startsWith("unix:")) {
			String path = address.substring(5);
			return NettyServerBuilder.forAddress(new DomainSocketAddress(path))
				.channelType(EpollServerDomainSocketChannel.class)
				.bossEventLoopGroup(new EpollEventLoopGroup(1))
				.workerEventLoopGroup(new EpollEventLoopGroup());
		}
		String host = super.hostname();
		int port = super.port();
		if (host == null || host.equals(GrpcUtils.ANY_IP_ADDRESS)) {
			logger.debug("Host for address %s is %s - creating builder w/ port %d only".formatted(address, host, port));
			return NettyServerBuilder.forPort(port, credentials());
		}
		logger.debug("Creating builder for address %s w/ host %s and port %d".formatted(address, host, port));
		SocketAddress socketAddress = new InetSocketAddress(host, port);
		return NettyServerBuilder.forAddress(socketAddress, credentials());
	}

}
