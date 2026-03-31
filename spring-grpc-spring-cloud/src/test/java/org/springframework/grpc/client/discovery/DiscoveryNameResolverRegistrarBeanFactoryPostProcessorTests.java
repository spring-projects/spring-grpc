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

package org.springframework.grpc.client.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;

import io.grpc.ManagedChannel;

/**
 * Tests for {@link DiscoveryNameResolverRegistrarBeanFactoryPostProcessor}.
 */
class DiscoveryNameResolverRegistrarBeanFactoryPostProcessorTests {

	@Test
	void addsRegistrarDependencyToGrpcChannelFactoryBeans() {
		var beanFactory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
		beanFactory.registerBeanDefinition("discoveryNameResolverRegistrar",
				new RootBeanDefinition(DiscoveryNameResolverRegistrar.class));
		RootBeanDefinition channelFactoryBeanDefinition = new RootBeanDefinition(TestGrpcChannelFactory.class);
		channelFactoryBeanDefinition.setDependsOn("existingDependency");
		beanFactory.registerBeanDefinition("grpcChannelFactory", channelFactoryBeanDefinition);
		var postProcessor = new DiscoveryNameResolverRegistrarBeanFactoryPostProcessor(
				"discoveryNameResolverRegistrar");
		postProcessor.postProcessBeanFactory(beanFactory);
		assertThat(beanFactory.getBeanDefinition("grpcChannelFactory").getDependsOn())
			.containsExactly("existingDependency", "discoveryNameResolverRegistrar");
	}

	static class TestGrpcChannelFactory implements GrpcChannelFactory {

		@Override
		public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean supports(String target) {
			return false;
		}

		@Override
		public boolean supports(io.grpc.ClientInterceptor interceptor) {
			return false;
		}

	}

}
