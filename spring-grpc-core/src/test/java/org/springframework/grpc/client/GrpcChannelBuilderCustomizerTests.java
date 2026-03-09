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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Tests for {@link GrpcChannelBuilderCustomizer}.
 */
class GrpcChannelBuilderCustomizerTests {

	@Test
	void matchingShouldApplyConsumerWhenTargetMatchesPattern() {
		String pattern = "localhost.*";
		@SuppressWarnings("unchecked")
		Consumer<ManagedChannelBuilder<NettyChannelBuilder>> consumer = mock(Consumer.class);
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		GrpcChannelBuilderCustomizer<NettyChannelBuilder> customizer = GrpcChannelBuilderCustomizer.matching(pattern,
				consumer);

		customizer.customize("localhost:8080", builder);

		verify(consumer).accept(builder);
	}

	@Test
	void matchingShouldNotApplyConsumerWhenTargetDoesNotMatchPattern() {
		String pattern = "localhost.*";
		@SuppressWarnings("unchecked")
		Consumer<ManagedChannelBuilder<NettyChannelBuilder>> consumer = mock(Consumer.class);
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		GrpcChannelBuilderCustomizer<NettyChannelBuilder> customizer = GrpcChannelBuilderCustomizer.matching(pattern,
				consumer);

		customizer.customize("example.com:8080", builder);

		verify(consumer, never()).accept(builder);
	}

	@Test
	void matchingShouldHandleExactMatch() {
		String pattern = "localhost";
		@SuppressWarnings("unchecked")
		Consumer<ManagedChannelBuilder<NettyChannelBuilder>> consumer = mock(Consumer.class);
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		GrpcChannelBuilderCustomizer<NettyChannelBuilder> customizer = GrpcChannelBuilderCustomizer.matching(pattern,
				consumer);

		customizer.customize("localhost", builder);

		verify(consumer).accept(builder);
	}

	@Test
	void matchingShouldHandleRegexPattern() {
		String pattern = ".*\\.example\\.com";
		@SuppressWarnings("unchecked")
		Consumer<ManagedChannelBuilder<NettyChannelBuilder>> consumer = mock(Consumer.class);
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		GrpcChannelBuilderCustomizer<NettyChannelBuilder> customizer = GrpcChannelBuilderCustomizer.matching(pattern,
				consumer);

		customizer.customize("api.example.com", builder);
		customizer.customize("service.example.com", builder);
		customizer.customize("other.com", builder);

		verify(consumer, times(2)).accept(builder);
	}

	@Test
	void defaultsShouldNotCustomizeBuilder() {
		@SuppressWarnings("unchecked")
		GrpcChannelBuilderCustomizer<NettyChannelBuilder> customizer = GrpcChannelBuilderCustomizer.defaults();
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		customizer.customize("localhost", builder);

		verifyNoInteractions(builder);
	}

	@Test
	void thenShouldChainCustomizers() {
		TestCustomizer customizer1 = new TestCustomizer();
		TestCustomizer customizer2 = new TestCustomizer();
		NettyChannelBuilder builder = mock(NettyChannelBuilder.class);

		GrpcChannelBuilderCustomizer<NettyChannelBuilder> chained = customizer1.then(customizer2);
		chained.customize("localhost", builder);

		assertThat(customizer1.getCallCount()).isOne();
		assertThat(customizer2.getCallCount()).isOne();
	}

	static class TestCustomizer implements GrpcChannelBuilderCustomizer<NettyChannelBuilder> {

		private int callCount;

		@Override
		public void customize(String target, NettyChannelBuilder builder) {
			this.callCount++;
		}

		int getCallCount() {
			return this.callCount;
		}

	}

}
