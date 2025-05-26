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

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.autoconfigure.client.ClientInterceptorsConfiguration;
import org.springframework.grpc.autoconfigure.client.GrpcClientAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.InProcessGrpcChannelFactory;
import org.springframework.grpc.server.InProcessGrpcServerFactory;
import org.springframework.grpc.server.ServerBuilderCustomizer;
import org.springframework.grpc.server.service.GrpcServiceDiscoverer;

import io.grpc.BindableService;
import io.grpc.inprocess.InProcessServerBuilder;

@AutoConfiguration(before = { GrpcServerFactoryAutoConfiguration.class, GrpcClientAutoConfiguration.class })
@ConditionalOnProperty(prefix = "spring.grpc.inprocess", name = "enabled", havingValue = "true")
@ConditionalOnClass(BindableService.class)
@Import(ClientInterceptorsConfiguration.class)
public class InProcessGrpcServerFactoryAutoConfiguration {

	private final String address = InProcessServerBuilder.generateName();

	@Bean
	@ConditionalOnBean(BindableService.class)
	InProcessGrpcServerFactory grpcServerFactory(GrpcServiceDiscoverer grpcServicesDiscoverer,
			List<ServerBuilderCustomizer<InProcessServerBuilder>> customizers) {
		InProcessGrpcServerFactory factory = new InProcessGrpcServerFactory(address, customizers);
		grpcServicesDiscoverer.findServices().forEach(factory::addService);
		return factory;
	}

	@Bean
	InProcessGrpcChannelFactory grpcChannelFactory(ClientInterceptorsConfigurer interceptorsConfigurer) {
		InProcessGrpcChannelFactory factory = new InProcessGrpcChannelFactory(interceptorsConfigurer);
		factory.setVirtualTargets(path -> address);
		return factory;
	}

}
