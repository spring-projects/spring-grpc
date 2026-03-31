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

package org.springframework.grpc.client.discovery.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.grpc.client.discovery.GrpcPortMetadataRegistrationBeanPostProcessor;

/**
 * Auto-configuration for publishing gRPC server metadata through Spring Cloud service
 * registration.
 *
 * @author Hyacinth Contributor
 */
@AutoConfiguration
@ConditionalOnClass(Registration.class)
@ConditionalOnProperty(prefix = "spring.grpc.server", name = "port")
@ConditionalOnProperty(prefix = "spring.grpc.server.discovery", name = "publish-metadata", havingValue = "true",
		matchIfMissing = true)
public final class GrpcDiscoveryServiceRegistryAutoConfiguration {

	private GrpcDiscoveryServiceRegistryAutoConfiguration() {
	}

	@Bean
	@ConditionalOnMissingBean
	static GrpcPortMetadataRegistrationBeanPostProcessor grpcPortMetadataRegistrationBeanPostProcessor(
			Environment environment) {
		int grpcPort = Integer.parseInt(environment.getRequiredProperty("spring.grpc.server.port"));
		return new GrpcPortMetadataRegistrationBeanPostProcessor(grpcPort);
	}

}
