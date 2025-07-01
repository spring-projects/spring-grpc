/*
 * Copyright 2024-2025 the original author or authors.
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

package org.springframework.grpc.server;

import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.lang.Nullable;

import io.grpc.TlsServerCredentials.ClientAuth;
import io.grpc.netty.NettyServerBuilder;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerDomainSocketChannel;
import io.netty.channel.unix.DomainSocketAddress;

/**
 * {@link GrpcServerFactory} that can be used to create a Netty-based gRPC server.
 *
 * @author David Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 */
public class NettyGrpcServerFactory extends DefaultGrpcServerFactory<NettyServerBuilder> {

	public NettyGrpcServerFactory(String address,
			List<ServerBuilderCustomizer<NettyServerBuilder>> serverBuilderCustomizers, KeyManagerFactory keyManager,
			TrustManagerFactory trustManager, ClientAuth clientAuth,
			@Nullable ServerServiceDefinitionFilter serviceFilter) {
		super(address, serverBuilderCustomizers, keyManager, trustManager, clientAuth, serviceFilter);
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
		return super.newServerBuilder();
	}

}
