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

package org.springframework.grpc.autoconfigure.server;

import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.netty.NettyServerBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.NettyGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.ShadedNettyGrpcServerFactory;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

/**
 * Configurations for {@link GrpcServerFactory gRPC server factories}.
 *
 * @author Chris Bono
 */
class GrpcServerFactoryConfigurations {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class ShadedNettyServerFactoryConfiguration {

		@Bean
		ShadedNettyGrpcServerFactory shadedNettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer grpcServicesDiscoverer, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			ShadedNettyServerFactoryPropertyMapper mapper = new ShadedNettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory.INSTANCE;
			}
			ShadedNettyGrpcServerFactory factory = new ShadedNettyGrpcServerFactory(properties.getAddress(),
					builderCustomizers, keyManager, trustManager, properties.getSsl().getClientAuth());
			grpcServicesDiscoverer.findServices().forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(ShadedNettyGrpcServerFactory.class)
		@Bean
		GrpcServerLifecycle shadedNettyGrpcServerLifecycle(ShadedNettyGrpcServerFactory factory, GrpcServerProperties properties,
				ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(NettyServerBuilder.class)
	@ConditionalOnMissingBean(GrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class NettyServerFactoryConfiguration {

		@Bean
		NettyGrpcServerFactory nettyGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer grpcServicesDiscoverer, ServerBuilderCustomizers serverBuilderCustomizers,
				SslBundles bundles) {
			NettyServerFactoryPropertyMapper mapper = new NettyServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<NettyServerBuilder>> builderCustomizers = List
				.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			KeyManagerFactory keyManager = null;
			TrustManagerFactory trustManager = null;
			if (properties.getSsl().isEnabled()) {
				SslBundle bundle = bundles.getBundle(properties.getSsl().getBundle());
				keyManager = bundle.getManagers().getKeyManagerFactory();
				trustManager = properties.getSsl().isSecure() ? bundle.getManagers().getTrustManagerFactory()
						: InsecureTrustManagerFactory.INSTANCE;
			}
			NettyGrpcServerFactory factory = new NettyGrpcServerFactory(properties.getAddress(), builderCustomizers,
					keyManager, trustManager, properties.getSsl().getClientAuth());
			grpcServicesDiscoverer.findServices().forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(NettyGrpcServerFactory.class)
		@Bean
		GrpcServerLifecycle nettyGrpcServerLifecycle(NettyGrpcServerFactory factory, GrpcServerProperties properties,
				ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(prefix = "spring.grpc.server.in-process", name = "name")
	@ConditionalOnClass(InProcessGrpcServerFactory.class)
	@EnableConfigurationProperties(GrpcServerProperties.class)
	static class InProcessServerFactoryConfiguration {

		@Bean
		InProcessGrpcServerFactory inProcessGrpcServerFactory(GrpcServerProperties properties,
				GrpcServiceDiscoverer grpcServicesDiscoverer, ServerBuilderCustomizers serverBuilderCustomizers) {
			var mapper = new InProcessServerFactoryPropertyMapper(properties);
			List<ServerBuilderCustomizer<InProcessServerBuilder>> builderCustomizers = List
					.of(mapper::customizeServerBuilder, serverBuilderCustomizers::customize);
			InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(properties.getInProcess().getName(), builderCustomizers);
			grpcServicesDiscoverer.findServices().forEach(factory::addService);
			return factory;
		}

		@ConditionalOnBean(InProcessGrpcServerFactory.class)
		@Bean
		GrpcServerLifecycle inProcessGrpcServerLifecycle(InProcessGrpcServerFactory factory, GrpcServerProperties properties,
				ApplicationEventPublisher eventPublisher) {
			return new GrpcServerLifecycle(factory, properties.getShutdownGracePeriod(), eventPublisher);
		}
	}
}
