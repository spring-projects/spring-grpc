/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.grpc.ServerCredentials;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.channel.unix.DomainSocketAddress;

/**
 * Unit tests for {@link ShadedNettyGrpcServerFactory}.
 *
 * @author Chris Bono
 */
class ShadedNettyGrpcServerFactoryTests {

	@Test
	@EnabledOnOs(OS.LINUX)
	void newServerBuilderUsesDomainSocketAddress() {
		var serverFactory = new ShadedNettyGrpcServerFactory("unix:/some/file/somewhere", Collections.emptyList(), null,
				null, null);
		try (MockedStatic<NettyServerBuilder> mockStaticServerBuilder = Mockito.mockStatic(NettyServerBuilder.class)) {
			mockStaticServerBuilder.when(() -> NettyServerBuilder.forAddress(any(SocketAddress.class)))
				.thenCallRealMethod();
			serverFactory.newServerBuilder();
			mockStaticServerBuilder
				.verify(() -> NettyServerBuilder.forAddress(eq(new DomainSocketAddress("/some/file/somewhere"))));
		}
	}

	@Test
	void newServerBuilderUsesPortOnlyWhenHostIsNull() {
		var serverFactory = new ShadedNettyGrpcServerFactory("/path/to/resource", Collections.emptyList(), null, null,
				null);
		try (MockedStatic<NettyServerBuilder> serverBuilder = Mockito.mockStatic(NettyServerBuilder.class)) {
			serverFactory.newServerBuilder();
			serverBuilder.verify(() -> NettyServerBuilder.forPort(eq(9090), any(ServerCredentials.class)));
		}
	}

	@Test
	void newServerBuilderUsesPortOnlyWhenHostIsWildcard() {
		var serverFactory = new ShadedNettyGrpcServerFactory("*:9090", Collections.emptyList(), null, null, null);
		try (MockedStatic<NettyServerBuilder> serverBuilder = Mockito.mockStatic(NettyServerBuilder.class)) {
			serverFactory.newServerBuilder();
			serverBuilder.verify(() -> NettyServerBuilder.forPort(eq(9090), any(ServerCredentials.class)));
		}
	}

	@Test
	void newServerBuilderUsesHostAndPortWhenHostSpecified() {
		var serverFactory = new ShadedNettyGrpcServerFactory("foo:9191", Collections.emptyList(), null, null, null);
		try (MockedStatic<NettyServerBuilder> serverBuilder = Mockito.mockStatic(NettyServerBuilder.class)) {
			serverFactory.newServerBuilder();
			serverBuilder.verify(() -> NettyServerBuilder.forAddress(eq(new InetSocketAddress("foo", 9191)),
					any(ServerCredentials.class)));
		}
	}

}
