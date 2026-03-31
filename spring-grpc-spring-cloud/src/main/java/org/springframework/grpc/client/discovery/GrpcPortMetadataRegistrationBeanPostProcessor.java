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

import java.util.Map;

import org.jspecify.annotations.NullMarked;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.core.log.LogAccessor;

/**
 * Publishes the configured gRPC server port into Spring Cloud service registration
 * metadata when the underlying {@link Registration} exposes a mutable metadata map.
 *
 * @author Hyacinth Contributor
 */
@NullMarked
public class GrpcPortMetadataRegistrationBeanPostProcessor implements BeanPostProcessor {

	private final LogAccessor logger = new LogAccessor(getClass());

	private final int grpcPort;

	public GrpcPortMetadataRegistrationBeanPostProcessor(int grpcPort) {
		this.grpcPort = grpcPort;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (!(bean instanceof Registration registration)) {
			return bean;
		}
		Map<String, String> metadata = registration.getMetadata();
		if (metadata == null) {
			this.logger
				.debug(() -> "Registration metadata is null, skipping gRPC metadata publication. beanName=" + beanName);
			return bean;
		}
		try {
			metadata.put(GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY, String.valueOf(this.grpcPort));
			this.logger.info(() -> "Published registration metadata " + GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY
					+ "=" + this.grpcPort + ". beanName=" + beanName);
		}
		catch (UnsupportedOperationException ex) {
			this.logger.debug(ex,
					() -> "Registration metadata is not mutable, skipping gRPC metadata publication. beanName="
							+ beanName);
		}
		return bean;
	}

}
