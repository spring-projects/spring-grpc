/*
 * Copyright 2025-2025 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.grpc.autoconfigure.client.GrpcClientAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.GrpcServerAutoConfiguration;
import org.springframework.grpc.autoconfigure.server.GrpcServerFactoryAutoConfiguration;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.grpc.server.GrpcServerFactory;

import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;

/**
 * Tests for {@link InProcessTestAutoConfiguration}.
 *
 * @author Chris Bono
 */
class InProcessTestAutoConfigurationTests {

	private final BindableService service = mock();

	private final ServerServiceDefinition serviceDefinition = ServerServiceDefinition.builder("my-service").build();

	@BeforeEach
	void prepareForTest() {
		when(service.bindService()).thenReturn(serviceDefinition);
	}

	private ApplicationContextRunner contextRunner() {
		return new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(InProcessTestAutoConfiguration.class,
					GrpcServerAutoConfiguration.class, GrpcServerFactoryAutoConfiguration.class,
					SslAutoConfiguration.class, GrpcClientAutoConfiguration.class))
			.withBean(BindableService.class, () -> service);
	}

	@Test
	void whenTestInProcessEnabledPropIsSetToTrueDoesAutoConfigureBeans() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.test.inprocess.enabled=true", "spring.grpc.server.inprocess.name=foo")
			.run((context) -> {
				assertThat(context).getBeans(GrpcServerFactory.class)
					.containsOnlyKeys("testInProcessGrpcServerFactory", "nettyGrpcServerFactory");
				assertThat(context).getBeans(GrpcChannelFactory.class)
					.containsOnlyKeys("testInProcessGrpcChannelFactory", "nettyGrpcChannelFactory");
			});
	}

	@Test
	void whenTestInProcessEnabledPropIsNotSetDoesNotAutoConfigureBeans() {
		this.contextRunner().withPropertyValues("spring.grpc.server.inprocess.name=foo").run((context) -> {
			assertThat(context).getBeans(GrpcServerFactory.class)
				.containsOnlyKeys("inProcessGrpcServerFactory", "nettyGrpcServerFactory");
			assertThat(context).getBeans(GrpcChannelFactory.class)
				.containsOnlyKeys("inProcessGrpcChannelFactory", "nettyGrpcChannelFactory");
		});
	}

	@Test
	void whenTestInProcessEnabledPropIsSetToFalseDoesNotAutoConfigureBeans() {
		this.contextRunner()
			.withPropertyValues("spring.grpc.test.inprocess.enabled=false", "spring.grpc.server.inprocess.name=foo")
			.run((context) -> {
				assertThat(context).getBeans(GrpcServerFactory.class)
					.containsOnlyKeys("inProcessGrpcServerFactory", "nettyGrpcServerFactory");
				assertThat(context).getBeans(GrpcChannelFactory.class)
					.containsOnlyKeys("inProcessGrpcChannelFactory", "nettyGrpcChannelFactory");
			});
	}

}
