/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultGrpcChannelFactory}.
 *
 * @author Andrey Litvitski
 */
class DefaultGrpcChannelFactoryTests {

	@Test
	void defaultTargetResolvesToLocalhost() {
		var factory = new DefaultGrpcChannelFactory<>(List.of(), mock());
		assertThat(factory.targets).isSameAs(VirtualTargets.DEFAULT);
		assertThat(factory.targets.getTarget("default")).isEqualTo("localhost:9090");
	}

}
