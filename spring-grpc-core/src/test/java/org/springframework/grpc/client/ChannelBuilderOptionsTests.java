/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.NettyChannelBuilder;

/**
 * Tests for {@link ChannelBuilderOptions}.
 */
class ChannelBuilderOptionsTests {

	@Test
	void defaultOptions() {
		var options = ChannelBuilderOptions.defaults();
		assertThat(options.interceptors()).isEmpty();
		assertThat(options.mergeWithGlobalInterceptors()).isFalse();
		assertThat(options.shutdownGracePeriod()).isEqualTo(Duration.ofSeconds(30));
		assertThat(options.customizer()).isNotNull();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	void userSpecifiedOptions() {
		ClientInterceptor interceptor1 = mock();
		ClientInterceptor interceptor2 = mock();
		GrpcChannelBuilderCustomizer customizer = mock();
		var options = ChannelBuilderOptions.defaults()
			.withInterceptors(List.of(interceptor1, interceptor2))
			.withInterceptorsMerge(true)
			.withShutdownGracePeriod(Duration.ofMinutes(1))
			.withCustomizer(customizer);
		assertThat(options.interceptors()).containsExactly(interceptor1, interceptor2);
		assertThat(options.mergeWithGlobalInterceptors()).isTrue();
		assertThat(options.shutdownGracePeriod()).isEqualTo(Duration.ofMinutes(1));
		assertThat(options.customizer()).isNotEqualTo(GrpcChannelBuilderCustomizer.defaults());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test
	void customizerApplied() {
		ManagedChannelBuilder builder = mock();
		GrpcChannelBuilderCustomizer customizer = mock();
		var options = ChannelBuilderOptions.defaults().withCustomizer(customizer);
		var applied = options.<ManagedChannelBuilder>customizer();
		applied.customize("localhost", builder);
		verify(customizer).customize("localhost", builder);
	}

	@Test
	void defaultOptionsCustomizerDoesNotCustomizerBuilder() {
		var customizer = ChannelBuilderOptions.defaults().<NettyChannelBuilder>customizer();
		var builder = mock(NettyChannelBuilder.class);
		customizer.customize("localhost", builder);
		verifyNoInteractions(builder);
	}

	@SuppressWarnings("rawtypes")
	@Nested
	class WithCustomizerUsageTests {

		@Test
		void rawBaseCustomizerLambda() {
			var options = ChannelBuilderOptions.defaults().withCustomizer((__, b) -> b.userAgent("foo"));
			assertThat(options.customizer()).isNotNull();
		}

		@Test
		void wildcardBaseCustomizer() {
			GrpcChannelBuilderCustomizer rawBaseCustomizer = (__, b) -> b.userAgent("foo");
			GrpcChannelBuilderCustomizer<?> wildcardBaseCustomizer = (GrpcChannelBuilderCustomizer<?>) rawBaseCustomizer;
			var options = ChannelBuilderOptions.defaults().withCustomizer(wildcardBaseCustomizer);
			assertThat(options.customizer()).isNotNull();
		}

		@Test
		void specificBuilderCustomizerLambda() {
			var options = ChannelBuilderOptions.defaults()
				.<NettyChannelBuilder>withCustomizer((__, b) -> b.flowControlWindow(5).userAgent("foo"));
			assertThat(options.customizer()).isNotNull();
		}

	}

}
