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

import java.util.function.Consumer;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;

/**
 * Callback interface that can be used to customize a {@link ManagedChannelBuilder} for a
 * specific target.
 *
 * @param <T> type of builder
 * @author Dave Syer
 * @author Chris Bono
 */
@FunctionalInterface
public interface GrpcChannelBuilderCustomizer<T extends ManagedChannelBuilder<T>> {

	/**
	 * Callback to customize a {@link ManagedChannelBuilder channel builder} instance for
	 * a specific target string. The target can be either a valid nameresolver-compliant
	 * URI or an authority string as described in
	 * {@link Grpc#newChannelBuilder(String, ChannelCredentials)}, or the name of a
	 * user-configured channel (e.g. 'my-channel').
	 * @param target the target string
	 * @param builder the builder to customize
	 */
	void customize(String target, T builder);

	default GrpcChannelBuilderCustomizer<T> then(GrpcChannelBuilderCustomizer<T> other) {
		return (target, builder) -> {
			customize(target, builder);
			other.customize(target, builder);
		};
	}

	/**
	 * Used to indicate no customizations should be made to the builder.
	 * @param <T> type of channel builder
	 * @return the default customizer
	 */
	static <T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> defaults() {
		return (__, ___) -> {
		};
	}

	static <T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> matching(String pattern,
			Consumer<ManagedChannelBuilder<T>> consumer) {
		return (target, channel) -> {
			if (target.matches(pattern)) {
				consumer.accept(channel);
			}
		};
	}

}
