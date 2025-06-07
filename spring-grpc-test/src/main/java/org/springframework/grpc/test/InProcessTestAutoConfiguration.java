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
package org.springframework.grpc.test;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.grpc.autoconfigure.client.ClientInterceptorsConfiguration;
import org.springframework.grpc.autoconfigure.client.GrpcClientAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.lifecycle.GrpcServerLifecycle;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

import io.grpc.BindableService;
import io.grpc.ChannelCredentials;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;

@AutoConfiguration(before = { GrpcServerFactoryAutoConfiguration.class, GrpcClientAutoConfiguration.class })
@ConditionalOnProperty(prefix = "spring.grpc.test.inprocess", name = "enabled", havingValue = "true")
@ConditionalOnClass(BindableService.class)
@Import(ClientInterceptorsConfiguration.class)
public class InProcessTestAutoConfiguration {

	private final String address = InProcessServerBuilder.generateName();

	@Bean
	@ConditionalOnBean(BindableService.class)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	TestInProcessGrpcServerFactory testInProcessGrpcServerFactory(GrpcServiceDiscoverer grpcServicesDiscoverer,
			List<ServerBuilderCustomizer<InProcessServerBuilder>> customizers) {
		var factory = new TestInProcessGrpcServerFactory(address, customizers);
		grpcServicesDiscoverer.findServices().forEach(factory::addService);
		return factory;
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	TestInProcessGrpcChannelFactory testInProcessGrpcChannelFactory(
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		return new TestInProcessGrpcChannelFactory(address, interceptorsConfigurer);
	}

	@Bean(name = "inProcessGrpcServerLifecycle")
	@ConditionalOnBean(InProcessGrpcServerFactory.class)
	@Order(Ordered.HIGHEST_PRECEDENCE)
	GrpcServerLifecycle inProcessGrpcServerLifecycle(InProcessGrpcServerFactory factory,
			ApplicationEventPublisher eventPublisher) {
		return new GrpcServerLifecycle(factory, Duration.ofSeconds(30), eventPublisher);
	}

	/**
	 * Specialization of {@link InProcessGrpcServerFactory}.
	 */
	public static class TestInProcessGrpcServerFactory extends InProcessGrpcServerFactory {

		public TestInProcessGrpcServerFactory(String address,
				List<ServerBuilderCustomizer<InProcessServerBuilder>> serverBuilderCustomizers) {
			super(address, serverBuilderCustomizers);
		}

	}

	/**
	 * Specialization of {@link InProcessGrpcChannelFactory} that allows the channel
	 * factory to support all targets, not just those that start with 'in-process:'.
	 */
	public static class TestInProcessGrpcChannelFactory extends InProcessGrpcChannelFactory {

		TestInProcessGrpcChannelFactory(String address, ClientInterceptorsConfigurer interceptorsConfigurer) {
			super(List.of(), interceptorsConfigurer);
			setVirtualTargets((path) -> address);
		}

		/**
		 * {@inheritDoc}
		 * @param target the target string as described in method javadocs
		 * @return {@code true} so that the test factory can handle all targets not just
		 * those prefixed with 'in-process:'
		 */
		@Override
		public boolean supports(String target) {
			return true;
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * Overrides the parent behavior so that the channel factory can handle all
		 * targets, not just those that prefixed with 'in-process:'.
		 * @param target the target of the channel
		 * @param creds the credentials for the channel which are ignored in this case
		 * @return a new inprocess channel builder instance
		 */
		@Override
		protected InProcessChannelBuilder newChannelBuilder(String target, ChannelCredentials creds) {
			if (target.startsWith("in-process:")) {
				return super.newChannelBuilder(target, creds);
			}
			return InProcessChannelBuilder.forName(target);
		}

	}

}
