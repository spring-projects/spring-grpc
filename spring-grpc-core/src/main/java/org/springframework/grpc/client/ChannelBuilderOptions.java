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

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import org.springframework.util.Assert;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannelBuilder;

/**
 * Options used by {@link GrpcChannelFactory} when building channels.
 * <p>
 * Provides functionality beyond what is available with the native channel builders (e.g.
 * {@code shutdownGracePeriod}) and overrides some native channel builder behavior (e.g.
 * {@code interceptors}.
 *
 * @author Chris Bono
 */
public final class ChannelBuilderOptions {

	private final List<ClientInterceptor> interceptors;

	private final boolean mergeWithGlobalInterceptors;

	private final Duration shutdownGracePeriod;

	@SuppressWarnings("rawtypes")
	private final GrpcChannelBuilderCustomizer customizer;

	@SuppressWarnings("rawtypes")
	private ChannelBuilderOptions(List<ClientInterceptor> interceptors, boolean mergeWithGlobalInterceptors,
			Duration shutdownGracePeriod, GrpcChannelBuilderCustomizer customizer) {
		this.interceptors = Collections.unmodifiableList(interceptors);
		this.mergeWithGlobalInterceptors = mergeWithGlobalInterceptors;
		this.shutdownGracePeriod = shutdownGracePeriod;
		this.customizer = customizer;
	}

	/**
	 * Gets the client interceptors to apply to the channel.
	 * @return the client interceptors to apply to the channel
	 */
	public List<ClientInterceptor> interceptors() {
		return this.interceptors;
	}

	/**
	 * Gets whether the provided interceptors should be blended with the global
	 * interceptors.
	 * @return whether the provided interceptors should be blended with the global
	 * interceptors (default false)
	 */
	public boolean mergeWithGlobalInterceptors() {
		return this.mergeWithGlobalInterceptors;
	}

	/**
	 * Gets the time to wait for the channel to gracefully shutdown.
	 * @return the time to wait for the channel to gracefully shutdown (default of 30s)
	 */
	public Duration shutdownGracePeriod() {
		return this.shutdownGracePeriod;
	}

	/**
	 * Gets the customizer to apply to the builder used to create the channel.
	 * @param <T> the type of the builder the customizer operates on
	 * @return the customizer to apply (default of
	 * {@link GrpcChannelBuilderCustomizer#defaults()})
	 */
	@SuppressWarnings("unchecked")
	public <T extends ManagedChannelBuilder<T>> GrpcChannelBuilderCustomizer<T> customizer() {
		return (GrpcChannelBuilderCustomizer<T>) this.customizer;
	}

	/**
	 * Gets a new immutable options instance populated with default values.
	 * @return a new immutable options instance populated with default values.
	 */
	public static ChannelBuilderOptions defaults() {
		return new ChannelBuilderOptions(List.of(), false, Duration.ofSeconds(30),
				GrpcChannelBuilderCustomizer.defaults());
	}

	/**
	 * Set the client interceptors to apply to the channel.
	 * @param interceptors list of client interceptors to apply to the channel or empty
	 * list to clear out any previously set interceptors
	 * @return a new immutable options instance populated with the specified interceptors
	 * and the settings of this current options instance.
	 */
	public ChannelBuilderOptions withInterceptors(List<ClientInterceptor> interceptors) {
		Assert.notNull(interceptors, "interceptors must not be null");
		return new ChannelBuilderOptions(interceptors, this.mergeWithGlobalInterceptors, this.shutdownGracePeriod,
				this.customizer);
	}

	/**
	 * Set whether the provided interceptors should be blended with the global
	 * interceptors.
	 * @param mergeWithGlobalInterceptors whether the provided interceptors should be
	 * @return a new immutable options instance populated with the specified merge setting
	 * and the settings of this current options instance.
	 */
	public ChannelBuilderOptions withInterceptorsMerge(boolean mergeWithGlobalInterceptors) {
		return new ChannelBuilderOptions(this.interceptors, mergeWithGlobalInterceptors, this.shutdownGracePeriod,
				this.customizer);
	}

	/**
	 * Set the time to wait for the channel to gracefully shutdown.
	 * @param shutdownGracePeriod the time to wait for the channel to gracefully shutdown
	 * @return a new immutable options instance populated with the specified
	 * {@code shutdownGracePeriod} and the settings of this current options instance.
	 */
	public ChannelBuilderOptions withShutdownGracePeriod(Duration shutdownGracePeriod) {
		return new ChannelBuilderOptions(this.interceptors, this.mergeWithGlobalInterceptors, shutdownGracePeriod,
				this.customizer);
	}

	/**
	 * Set the customizer to apply to the builder used to create the channel.
	 * @param <T> type of builder the customizer operates on
	 * @param customizer the customizer to apply to the builder used to create the channel
	 * @return a new immutable options instance populated with the specified
	 * {@code customizer} and the settings of this current options instance.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ManagedChannelBuilder<T>> ChannelBuilderOptions withCustomizer(
			GrpcChannelBuilderCustomizer<T> customizer) {
		return new ChannelBuilderOptions(this.interceptors, this.mergeWithGlobalInterceptors, this.shutdownGracePeriod,
				this.customizer.then(customizer));
	}

}
