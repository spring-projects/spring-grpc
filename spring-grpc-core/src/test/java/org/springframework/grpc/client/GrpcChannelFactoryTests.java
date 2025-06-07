/*
 * Copyright 2023-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Tests for the various {@link GrpcChannelFactory} implementations.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class GrpcChannelFactoryTests {

	ManagedChannel channel;

	@AfterEach
	void closeChannel() {
		if (this.channel != null) {
			this.channel.shutdownNow();
		}
	}

	@Nested
	class CreateChannelApiWithCustomizers {

		@Test
		void globalCustomizersInvokedInOrder() {
			var channelName = "localhost";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var customizer2 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new DefaultGrpcChannelFactory(List.of(customizer1, customizer2), mock());
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName);
			assertThat(channel).isNotNull();
			var inOrder = inOrder(customizer1, customizer2);
			inOrder.verify(customizer1).customize(anyString(), any(ManagedChannelBuilder.class));
			inOrder.verify(customizer2).customize(anyString(), any(ManagedChannelBuilder.class));
		}

		@Test
		void whenOptionsContainCustomizerThenCustomizerInvoked() {
			var channelName = "localhost";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new DefaultGrpcChannelFactory(List.of(), mock());
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults().withCustomizer(customizer1));
			assertThat(channel).isNotNull();
			verify(customizer1).customize(anyString(), any(ManagedChannelBuilder.class));
		}

		@Test
		void globalCustomizersInvokedBeforeSpecificCustomizer() {
			var channelName = "localhost";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var customizer2 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new DefaultGrpcChannelFactory(List.of(customizer2), mock());
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults().withCustomizer(customizer1));
			assertThat(channel).isNotNull();
			var inOrder = inOrder(customizer1, customizer2);
			inOrder.verify(customizer2).customize(anyString(), any(ManagedChannelBuilder.class));
			inOrder.verify(customizer1).customize(anyString(), any(ManagedChannelBuilder.class));
		}

	}

	@Nested
	class CreateChannelApiWithInterceptors {

		@Test
		void whenOptionsContainNoInterceptorThenConfigurerInvokedWithNoInterceptor() {
			ClientInterceptorsConfigurer configurer = mock();
			var channelName = "localhost";
			var channelFactory = new DefaultGrpcChannelFactory(List.of(), configurer);
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName);
			assertThat(channel).isNotNull();
			verify(configurer).configureInterceptors(any(ManagedChannelBuilder.class),
					assertArg((interceptors) -> assertThat(interceptors).isEmpty()), eq(false));
		}

		@Test
		void whenOptionsContainInterceptorThenConfigurerInvokedWithInterceptor() {
			ClientInterceptorsConfigurer configurer = mock();
			var channelName = "localhost";
			var channelFactory = new DefaultGrpcChannelFactory(List.of(), configurer);
			channelFactory.setVirtualTargets(path -> path);
			var interceptor = mock(ClientInterceptor.class);
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults()
						.withInterceptors(List.of(interceptor))
						.withInterceptorsMerge(true));
			assertThat(channel).isNotNull();
			verify(configurer).configureInterceptors(any(ManagedChannelBuilder.class),
					assertArg((interceptors) -> assertThat(interceptors).containsExactly(interceptor)), eq(true));
		}

	}

	@Nested
	class SpecificGrpcChannelFactoryTests {

		@Test
		void nettyChannelFactoryUsesNettyChannelBuilder() {
			var channelName = "localhost";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new NettyGrpcChannelFactory(List.of(), mock());
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults().withCustomizer(customizer1));
			assertThat(channel).isNotNull();
			verify(customizer1).customize(anyString(), ArgumentMatchers
				.assertArg((builder) -> assertThat(builder).isInstanceOf(NettyChannelBuilder.class)));
		}

		@Test
		void shadedNettyChannelFactoryUsesShadedNettyChannelBuilder() {
			var channelName = "localhost";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new ShadedNettyGrpcChannelFactory(List.of(), mock());
			channelFactory.setVirtualTargets(path -> path);
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults().withCustomizer(customizer1));
			assertThat(channel).isNotNull();
			verify(customizer1).customize(anyString(), ArgumentMatchers.assertArg((builder) -> assertThat(builder)
				.isInstanceOf(io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class)));
		}

		@Test
		void inProcessChannelFactoryUsesInProcessChannelBuilder() {
			var channelName = "in-process:foo";
			var customizer1 = mock(GrpcChannelBuilderCustomizer.class);
			var channelFactory = new InProcessGrpcChannelFactory(List.of(), mock());
			channel = channelFactory.createChannel(channelName,
					ChannelBuilderOptions.defaults().withCustomizer(customizer1));
			assertThat(channel).isNotNull();
			verify(customizer1).customize(anyString(),
					ArgumentMatchers
						.assertArg((builder) -> assertThat(builder).isInstanceOf(InProcessChannelBuilder.class)
							.extracting("managedChannelImplBuilder.target")
							.isEqualTo("directaddress:///foo")));
			// NOTE: the impl target ending in foo proves the original target was stripped
			// of 'in-process:' prefix
		}

	}

	@Nested
	class SupportsApiTests {

		@Test
		void defaultSupportsEverythingExceptInProcess() {
			var channelFactory = new DefaultGrpcChannelFactory(List.of(), mock());
			assertThat(channelFactory.supports("foo")).isTrue();
			assertThat(channelFactory.supports("static:127.0.0.1")).isTrue();
			assertThat(channelFactory.supports("in-process:foo")).isFalse();
		}

		@Test
		void nettySupportsEverythingExceptInProcess() {
			var channelFactory = new NettyGrpcChannelFactory(List.of(), mock());
			assertThat(channelFactory.supports("foo")).isTrue();
			assertThat(channelFactory.supports("static:127.0.0.1")).isTrue();
			assertThat(channelFactory.supports("in-process:foo")).isFalse();
		}

		@Test
		void shadedNettySupportsEverythingExceptInProcess() {
			var channelFactory = new ShadedNettyGrpcChannelFactory(List.of(), mock());
			assertThat(channelFactory.supports("foo")).isTrue();
			assertThat(channelFactory.supports("static:127.0.0.1")).isTrue();
			assertThat(channelFactory.supports("in-process:foo")).isFalse();
		}

		@Test
		void inProcessSupportsOnlyInProcess() {
			var channelFactory = new InProcessGrpcChannelFactory(List.of(), mock());
			assertThat(channelFactory.supports("foo")).isFalse();
			assertThat(channelFactory.supports("static:127.0.0.1")).isFalse();
			assertThat(channelFactory.supports("in-process:foo")).isTrue();
		}

	}

}
