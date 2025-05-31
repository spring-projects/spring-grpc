/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.grpc.autoconfigure.client;

import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.channel.Channel;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ChannelCredentialsProvider;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.client.InProcessOrAlternativeChannelFactory;
import org.springframework.grpc.client.NettyGrpcChannelFactory;
import org.springframework.grpc.client.ShadedNettyGrpcChannelFactory;

/**
 * Configurations for {@link GrpcChannelFactory gRPC channel factories}.
 *
 * @author Chris Bono
 */
class GrpcChannelFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(value = { io.grpc.netty.shaded.io.netty.channel.Channel.class,
			io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class,
			InProcessGrpcChannelFactory.class })
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class ShadedNettyAndInProcessChannelFactoryConfiguration {

		@Bean
		InProcessOrAlternativeChannelFactory shadedNettyWithInProcessGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder>> builderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var factory = new ShadedNettyGrpcChannelFactory(builderCustomizers, interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);

			// TODO: filter out GCI that have includeInProcess=false
			List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> inProcessBuilderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var inProcessFactory = new InProcessGrpcChannelFactory(inProcessBuilderCustomizers,
					interceptorsConfigurer);
			inProcessFactory.setVirtualTargets(p -> p);
			return new InProcessOrAlternativeChannelFactory(inProcessFactory, factory);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(value = { io.grpc.netty.shaded.io.netty.channel.Channel.class,
			io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder.class })
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class ShadedNettyOnlyChannelFactoryConfiguration {

		@Bean
		ShadedNettyGrpcChannelFactory shadedNettyWithoutProcessGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder>> builderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var factory = new ShadedNettyGrpcChannelFactory(builderCustomizers, interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);
			return factory;
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(value = { Channel.class, NettyChannelBuilder.class, InProcessGrpcChannelFactory.class } )
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class NettyAndInProcessChannelFactoryConfiguration {

		@Bean
		InProcessOrAlternativeChannelFactory nettyGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<NettyChannelBuilder>> builderCustomizers = List
				.of(channelBuilderCustomizers::customize);
			var factory = new NettyGrpcChannelFactory(builderCustomizers, interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);

			List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> inProcessBuilderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var inProcessFactory = new InProcessGrpcChannelFactory(inProcessBuilderCustomizers, interceptorsConfigurer);
			inProcessFactory.setVirtualTargets(p -> p);
			return new InProcessOrAlternativeChannelFactory(inProcessFactory, factory);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(value = { Channel.class, NettyChannelBuilder.class} )
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class NettyOnlyChannelFactoryConfiguration {

		@Bean
		NettyGrpcChannelFactory nettyGrpcChannelFactory(GrpcClientProperties properties,
				ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer, ChannelCredentialsProvider credentials) {
			List<GrpcChannelBuilderCustomizer<NettyChannelBuilder>> builderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var factory = new NettyGrpcChannelFactory(builderCustomizers, interceptorsConfigurer);
			factory.setCredentialsProvider(credentials);
			factory.setVirtualTargets(properties);
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(InProcessChannelBuilder.class)
	@ConditionalOnMissingBean(GrpcChannelFactory.class)
	@EnableConfigurationProperties(GrpcClientProperties.class)
	static class InProcessOnlyChannelFactoryConfiguration {

		@Bean
		InProcessGrpcChannelFactory inProcessGrpcChannelFactory(ChannelBuilderCustomizers channelBuilderCustomizers,
				ClientInterceptorsConfigurer interceptorsConfigurer) {
			List<GrpcChannelBuilderCustomizer<InProcessChannelBuilder>> inProcessBuilderCustomizers = List
					.of(channelBuilderCustomizers::customize);
			var inProcessFactory = new InProcessGrpcChannelFactory(inProcessBuilderCustomizers, interceptorsConfigurer);
			inProcessFactory.setVirtualTargets(p -> p);
			return inProcessFactory;
		}

	}
}
