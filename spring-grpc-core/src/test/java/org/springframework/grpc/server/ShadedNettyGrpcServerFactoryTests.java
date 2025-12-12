/*
 * Copyright 2025-present the original author or authors.
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
