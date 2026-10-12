/*
 * Copyright 2024-present the original author or authors.
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

package org.springframework.grpc.transcoding.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.converter.protobuf.ProtobufJsonFormatHttpMessageConverter;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;

/**
 * Tests for {@link GrpcTranscodingConfiguration}.
 *
 * @author Aleksander Brzozowski
 */
class GrpcTranscodingConfigurationTests {

	@Test
	void registersRuntimeInfrastructureWithDefaultInProcessName() {
		GrpcChannelFactory channelFactory = mock(GrpcChannelFactory.class);
		ManagedChannel channel = mock(ManagedChannel.class);
		ArgumentCaptor<ChannelBuilderOptions> optionsCaptor = ArgumentCaptor.forClass(ChannelBuilderOptions.class);
		when(channelFactory.createChannel(eq("in-process:grpc-transcoding"), optionsCaptor.capture()))
			.thenReturn(channel);

		try (AnnotationConfigApplicationContext context = applicationContext(channelFactory, Map.of())) {
			assertThat(context.getBean("grpcTranscodingChannel", Channel.class)).isSameAs(channel);
			assertThat(context.getBean(ProtobufJsonFormatHttpMessageConverter.class)).isNotNull();
		}

		verify(channelFactory).createChannel(eq("in-process:grpc-transcoding"), any(ChannelBuilderOptions.class));
		ChannelBuilderOptions options = optionsCaptor.getValue();
		assertThat(options.mergeWithGlobalInterceptors()).isTrue();
		InProcessChannelBuilder builder = mock(InProcessChannelBuilder.class);
		options.<InProcessChannelBuilder>customizer().customize("in-process:grpc-transcoding", builder);
		verify(builder).directExecutor();
	}

	@Test
	void usesCustomInProcessNameFromEnvironment() {
		GrpcChannelFactory channelFactory = mock(GrpcChannelFactory.class);
		ManagedChannel channel = mock(ManagedChannel.class);
		when(channelFactory.createChannel(eq("in-process:custom-server"), any(ChannelBuilderOptions.class)))
			.thenReturn(channel);

		try (AnnotationConfigApplicationContext context = applicationContext(channelFactory,
				Map.of("spring.grpc.transcoding.in-process-name", "custom-server"))) {
			assertThat(context.getBean("grpcTranscodingChannel", Channel.class)).isSameAs(channel);
		}

		verify(channelFactory).createChannel(eq("in-process:custom-server"), any(ChannelBuilderOptions.class));
	}

	private AnnotationConfigApplicationContext applicationContext(GrpcChannelFactory channelFactory,
			Map<String, Object> properties) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", properties));
		context.registerBean(GrpcChannelFactory.class, () -> channelFactory);
		context.register(GrpcTranscodingConfiguration.class);
		context.refresh();
		return context;
	}

}
