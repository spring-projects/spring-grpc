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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.grpc.autoconfigure.client.ClientScanConfiguration.DefaultGrpcClientRegistrations;
import org.springframework.grpc.autoconfigure.client.GrpcClientProperties.ChannelConfig;
import org.springframework.grpc.client.AbstractGrpcClientRegistrar;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.GrpcClientFactory.GrpcClientRegistrationSpec;
import org.springframework.grpc.client.GrpcClientFactoryPostProcessor;

@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(GrpcClientFactoryPostProcessor.class)
@Import(DefaultGrpcClientRegistrations.class)
public class ClientScanConfiguration {

	static class DefaultGrpcClientRegistrations extends AbstractGrpcClientRegistrar
			implements EnvironmentAware, BeanFactoryAware {

		private Environment environment;

		private BeanFactory beanFactory;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Override
		public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
			this.beanFactory = beanFactory;
		}

		@Override
		protected GrpcClientRegistrationSpec[] collect(AnnotationMetadata meta) {
			Binder binder = Binder.get(environment);
			boolean hasDefaultChannel = binder.bind("spring.grpc.client.default-channel", ChannelConfig.class)
				.isBound();
			if (hasDefaultChannel) {
				List<String> packages = new ArrayList<>();
				if (AutoConfigurationPackages.has(beanFactory)) {
					packages.addAll(AutoConfigurationPackages.get(beanFactory));
				}
				// TODO: change global default factory type in properties maybe?
				return new GrpcClientRegistrationSpec[] { GrpcClientRegistrationSpec.of("default")
					.factory(BlockingStubFactory.class)
					.packages(packages.toArray(new String[0])) };
			}
			return new GrpcClientRegistrationSpec[0];
		}

	}

}
