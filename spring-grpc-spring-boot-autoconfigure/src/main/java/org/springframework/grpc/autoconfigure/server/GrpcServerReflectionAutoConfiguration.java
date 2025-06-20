/*
 * Copyright 2023-2025 the original author or authors.
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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionServiceV1;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC Reflection service
 * <p>
 * This auto-configuration is enabled by default. To disable it, set the configuration
 * flag {spring.grpc.server.reflection.enabled=false} in your application properties.
 *
 * @author Haris Zujo
 */
@AutoConfiguration(before = GrpcServerFactoryAutoConfiguration.class)
@ConditionalOnGrpcServerEnabled
@ConditionalOnClass(ProtoReflectionServiceV1.class)
@ConditionalOnProperty(name = "spring.grpc.server.reflection.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(BindableService.class)
public class GrpcServerReflectionAutoConfiguration {

	@Bean
	public BindableService serverReflection() {
		return ProtoReflectionServiceV1.newInstance();
	}

}
