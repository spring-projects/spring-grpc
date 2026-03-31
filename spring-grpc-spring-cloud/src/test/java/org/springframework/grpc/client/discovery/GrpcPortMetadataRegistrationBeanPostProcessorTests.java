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

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.serviceregistry.Registration;

/**
 * Tests for {@link GrpcPortMetadataRegistrationBeanPostProcessor}.
 */
class GrpcPortMetadataRegistrationBeanPostProcessorTests {

	@Test
	void publishesGrpcPortMetadataToRegistration() {
		var registration = new TestRegistration();
		var postProcessor = new GrpcPortMetadataRegistrationBeanPostProcessor(9091);
		postProcessor.postProcessAfterInitialization(registration, "registration");
		assertThat(registration.getMetadata()).containsEntry(GrpcDiscoveryConstants.GRPC_PORT_METADATA_KEY, "9091");
	}

	static class TestRegistration implements Registration {

		private final Map<String, String> metadata = new LinkedHashMap<>();

		@Override
		public String getServiceId() {
			return "test-service";
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return 8080;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public URI getUri() {
			return URI.create("http://127.0.0.1:8080");
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.metadata;
		}

	}

}
