/*
 * Copyright 2023-2024 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.grpc.ManagedChannel;

/**
 * Unit tests for the {@link CompositeGrpcChannelFactory}.
 */
@SuppressWarnings({ "unchecked", "rawtypes" })
class CompositeGrpcChannelFactoryTests {

	private TestChannelFactory fooChannelFactory;

	private TestChannelFactory barChannelFactory;

	private CompositeGrpcChannelFactory compositeChannelFactory;

	@BeforeEach
	void prepareFactories() {
		this.fooChannelFactory = new TestChannelFactory("foo");
		this.barChannelFactory = new TestChannelFactory("bar");
		this.compositeChannelFactory = new CompositeGrpcChannelFactory(List.of(fooChannelFactory, barChannelFactory));
	}

	@Test
	void atLeastOneChannelFactoryRequired() {
		assertThatIllegalArgumentException().isThrownBy(() -> new CompositeGrpcChannelFactory(null))
			.withMessage("composite channel factory requires at least one channel factory");
		assertThatIllegalArgumentException().isThrownBy(() -> new CompositeGrpcChannelFactory(List.of()))
			.withMessage("composite channel factory requires at least one channel factory");
	}

	@Test
	void supportsDependsOnSupportsOfComposedFactories() {
		assertThat(compositeChannelFactory.supports("foo")).isTrue();
		assertThat(compositeChannelFactory.supports("bar")).isTrue();
		assertThat(compositeChannelFactory.supports("zaa")).isFalse();
	}

	@Test
	void firstComposedChannelFactorySupportsTarget() {
		assertThat(compositeChannelFactory.createChannel("foo")).isNotNull();
		assertThat(fooChannelFactory.getActualTarget()).isEqualTo("foo");
		assertThat(barChannelFactory.getActualTarget()).isNull();
	}

	@Test
	void secondComposedChannelFactorySupportsTarget() {
		assertThat(compositeChannelFactory.createChannel("bar")).isNotNull();
		assertThat(fooChannelFactory.getActualTarget()).isNull();
		assertThat(barChannelFactory.getActualTarget()).isEqualTo("bar");
	}

	@Test
	void noComposedChannelFactorySupportsTarget() {
		assertThatIllegalStateException().isThrownBy(() -> compositeChannelFactory.createChannel("zaa"))
			.withMessage("No grpc channel factory found that supports target : zaa");
		assertThat(fooChannelFactory.getActualTarget()).isNull();
		assertThat(barChannelFactory.getActualTarget()).isNull();
	}

	static class TestChannelFactory implements GrpcChannelFactory {

		private String expectedTarget;

		private String actualTarget;

		TestChannelFactory(String expectedTarget) {
			this.expectedTarget = expectedTarget;
		}

		public boolean supports(String target) {
			return target.equals(this.expectedTarget);
		}

		@Override
		public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
			this.actualTarget = target;
			return mock();
		}

		String getActualTarget() {
			return this.actualTarget;
		}

	}

}
