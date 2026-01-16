/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.client.autoconfigure;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.NegotiationType;
import org.springframework.grpc.client.StubFactory;
import org.springframework.grpc.client.VirtualTargets;
import org.springframework.util.unit.DataSize;

import io.grpc.ManagedChannel;

/**
 * Configuration properties for the gRPC client side.
 *
 * @author Dave Syer
 * @author Chris Bono
 * @author Vahid Ramezani
 */
@ConfigurationProperties(prefix = "spring.grpc.client")
public class GrpcClientProperties implements EnvironmentAware, VirtualTargets {

	/**
	 * Map of channels configured by name.
	 */
	private final Map<String, ChannelConfig> channels = new HashMap<>();

	/**
	 * The default channel configuration to use for new channels.
	 */
	private final ChannelConfig defaultChannel = new ChannelConfig();

	/**
	 * Default stub factory to use for all channels.
	 */
	private Class<? extends StubFactory<?>> defaultStubFactory = BlockingStubFactory.class;

	private Environment environment;

	GrpcClientProperties() {
		this.environment = new StandardEnvironment();
	}

	public Map<String, ChannelConfig> getChannels() {
		return this.channels;
	}

	public ChannelConfig getDefaultChannel() {
		return this.defaultChannel;
	}

	public Class<? extends StubFactory<?>> getDefaultStubFactory() {
		return this.defaultStubFactory;
	}

	public void setDefaultStubFactory(Class<? extends StubFactory<?>> defaultStubFactory) {
		this.defaultStubFactory = defaultStubFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Gets the configured channel with the given name. If no channel is configured for
	 * the specified name then one is created using the default channel as a template.
	 * Named channels inherit settings from the default channel, with explicitly
	 * configured values taking precedence.
	 * @param name the name of the channel
	 * @return the configured channel if found (merged with defaults), or a newly created
	 * channel using the default channel as a template
	 */
	public ChannelConfig getChannel(String name) {
		if ("default".equals(name)) {
			return this.defaultChannel;
		}
		ChannelConfig channel = this.channels.get(name);
		if (channel != null) {
			return this.defaultChannel.mergeWith(channel);
		}
		channel = this.defaultChannel.copy();
		String address = name;
		if (!name.contains(":/") && !name.startsWith("unix:")) {
			if (name.contains(":")) {
				address = "static://" + name;
			}
			else {
				address = this.defaultChannel.getAddress();
				if (!address.contains(":/")) {
					address = "static://" + address;
				}
			}
		}
		channel.setAddress(address);
		return channel;
	}

	@Override
	public String getTarget(String authority) {
		ChannelConfig channel = this.getChannel(authority);
		String address = channel.getAddress();
		if (address.startsWith("static:") || address.startsWith("tcp:")) {
			address = address.substring(address.indexOf(":") + 1).replaceFirst("/*", "");
		}
		return this.environment.resolvePlaceholders(address);
	}

	/**
	 * Represents the configuration for a {@link ManagedChannel gRPC channel}.
	 */
	public static class ChannelConfig {

		private static final String DEFAULT_ADDRESS = "static://localhost:9090";

		private static final String DEFAULT_LOAD_BALANCING_POLICY = "round_robin";

		private static final boolean DEFAULT_ENABLE_KEEP_ALIVE = false;

		private static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofSeconds(20);

		private static final Duration DEFAULT_KEEP_ALIVE_TIME = Duration.ofMinutes(5);

		private static final Duration DEFAULT_KEEP_ALIVE_TIMEOUT = Duration.ofSeconds(20);

		private static final boolean DEFAULT_KEEP_ALIVE_WITHOUT_CALLS = false;

		private static final DataSize DEFAULT_MAX_INBOUND_MESSAGE_SIZE = DataSize.ofBytes(4194304);

		private static final DataSize DEFAULT_MAX_INBOUND_METADATA_SIZE = DataSize.ofBytes(8192);

		private static final NegotiationType DEFAULT_NEGOTIATION_TYPE = NegotiationType.PLAINTEXT;

		private static final boolean DEFAULT_SECURE = true;

		/**
		 * The target address uri to connect to.
		 */
		private @Nullable String address;

		/**
		 * The default deadline for RPCs performed on this channel.
		 */
		private @Nullable Duration defaultDeadline;

		/**
		 * The load balancing policy the channel should use.
		 */
		private @Nullable String defaultLoadBalancingPolicy;

		/**
		 * Whether keep alive is enabled on the channel.
		 */
		private @Nullable Boolean enableKeepAlive;

		private final Health health = new Health();

		/**
		 * The duration without ongoing RPCs before going to idle mode.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration idleTimeout;

		/**
		 * The delay before sending a keepAlive. Note that shorter intervals increase the
		 * network burden for the server and this value can not be lower than
		 * 'permitKeepAliveTime' on the server.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration keepAliveTime;

		/**
		 * The default timeout for a keepAlives ping request.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private @Nullable Duration keepAliveTimeout;

		/**
		 * Whether a keepAlive will be performed when there are no outstanding RPC on a
		 * connection.
		 */
		private @Nullable Boolean keepAliveWithoutCalls;

		/**
		 * Maximum message size allowed to be received by the channel (default 4MiB). Set
		 * to '-1' to use the highest possible limit (not recommended).
		 */
		private @Nullable DataSize maxInboundMessageSize;

		/**
		 * Maximum metadata size allowed to be received by the channel (default 8KiB). Set
		 * to '-1' to use the highest possible limit (not recommended).
		 */
		private @Nullable DataSize maxInboundMetadataSize;

		/**
		 * The negotiation type for the channel.
		 */
		private @Nullable NegotiationType negotiationType;

		/**
		 * Flag to say that strict SSL checks are not enabled (so the remote certificate
		 * could be anonymous).
		 */
		private @Nullable Boolean secure;

		/**
		 * Map representation of the service config to use for the channel.
		 */
		private final Map<String, Object> serviceConfig = new HashMap<>();

		private final Ssl ssl = new Ssl();

		/**
		 * The custom User-Agent for the channel.
		 */
		private @Nullable String userAgent;

		public String getAddress() {
			return Objects.requireNonNullElse(this.address, DEFAULT_ADDRESS);
		}

		public void setAddress(final String address) {
			this.address = address;
		}

		public @Nullable Duration getDefaultDeadline() {
			return this.defaultDeadline;
		}

		public void setDefaultDeadline(@Nullable Duration defaultDeadline) {
			this.defaultDeadline = defaultDeadline;
		}

		public String getDefaultLoadBalancingPolicy() {
			return Objects.requireNonNullElse(this.defaultLoadBalancingPolicy, DEFAULT_LOAD_BALANCING_POLICY);
		}

		public void setDefaultLoadBalancingPolicy(final String defaultLoadBalancingPolicy) {
			this.defaultLoadBalancingPolicy = defaultLoadBalancingPolicy;
		}

		public boolean isEnableKeepAlive() {
			return Objects.requireNonNullElse(this.enableKeepAlive, DEFAULT_ENABLE_KEEP_ALIVE);
		}

		public void setEnableKeepAlive(boolean enableKeepAlive) {
			this.enableKeepAlive = enableKeepAlive;
		}

		public Health getHealth() {
			return this.health;
		}

		public Duration getIdleTimeout() {
			return Objects.requireNonNullElse(this.idleTimeout, DEFAULT_IDLE_TIMEOUT);
		}

		public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Duration getKeepAliveTime() {
			return Objects.requireNonNullElse(this.keepAliveTime, DEFAULT_KEEP_ALIVE_TIME);
		}

		public void setKeepAliveTime(Duration keepAliveTime) {
			this.keepAliveTime = keepAliveTime;
		}

		public Duration getKeepAliveTimeout() {
			return Objects.requireNonNullElse(this.keepAliveTimeout, DEFAULT_KEEP_ALIVE_TIMEOUT);
		}

		public void setKeepAliveTimeout(Duration keepAliveTimeout) {
			this.keepAliveTimeout = keepAliveTimeout;
		}

		public boolean isKeepAliveWithoutCalls() {
			return Objects.requireNonNullElse(this.keepAliveWithoutCalls, DEFAULT_KEEP_ALIVE_WITHOUT_CALLS);
		}

		public void setKeepAliveWithoutCalls(boolean keepAliveWithoutCalls) {
			this.keepAliveWithoutCalls = keepAliveWithoutCalls;
		}

		public DataSize getMaxInboundMessageSize() {
			return Objects.requireNonNullElse(this.maxInboundMessageSize, DEFAULT_MAX_INBOUND_MESSAGE_SIZE);
		}

		public void setMaxInboundMessageSize(final DataSize maxInboundMessageSize) {
			this.setMaxInboundSize(maxInboundMessageSize, (s) -> this.maxInboundMessageSize = s,
					"maxInboundMessageSize");
		}

		public DataSize getMaxInboundMetadataSize() {
			return Objects.requireNonNullElse(this.maxInboundMetadataSize, DEFAULT_MAX_INBOUND_METADATA_SIZE);
		}

		public void setMaxInboundMetadataSize(DataSize maxInboundMetadataSize) {
			this.setMaxInboundSize(maxInboundMetadataSize, (s) -> this.maxInboundMetadataSize = s,
					"maxInboundMetadataSize");
		}

		private void setMaxInboundSize(DataSize maxSize, Consumer<DataSize> setter, String propertyName) {
			if (maxSize != null && maxSize.toBytes() >= 0) {
				setter.accept(maxSize);
			}
			else if (maxSize != null && maxSize.toBytes() == -1) {
				setter.accept(DataSize.ofBytes(Integer.MAX_VALUE));
			}
			else {
				throw new IllegalArgumentException("Unsupported %s: %s".formatted(propertyName, maxSize));
			}
		}

		public NegotiationType getNegotiationType() {
			return Objects.requireNonNullElse(this.negotiationType, DEFAULT_NEGOTIATION_TYPE);
		}

		public void setNegotiationType(NegotiationType negotiationType) {
			this.negotiationType = negotiationType;
		}

		public boolean isSecure() {
			return Objects.requireNonNullElse(this.secure, DEFAULT_SECURE);
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public Map<String, Object> getServiceConfig() {
			return this.serviceConfig;
		}

		public Ssl getSsl() {
			return this.ssl;
		}

		public @Nullable String getUserAgent() {
			return this.userAgent;
		}

		public void setUserAgent(@Nullable String userAgent) {
			this.userAgent = userAgent;
		}

		/**
		 * Provide a copy of the channel instance.
		 * @return a copy of the channel instance.
		 */
		ChannelConfig copy() {
			ChannelConfig copy = new ChannelConfig();
			copy.address = this.address;
			copy.defaultLoadBalancingPolicy = this.defaultLoadBalancingPolicy;
			copy.negotiationType = this.negotiationType;
			copy.enableKeepAlive = this.enableKeepAlive;
			copy.idleTimeout = this.idleTimeout;
			copy.keepAliveTime = this.keepAliveTime;
			copy.keepAliveTimeout = this.keepAliveTimeout;
			copy.keepAliveWithoutCalls = this.keepAliveWithoutCalls;
			copy.maxInboundMessageSize = this.maxInboundMessageSize;
			copy.maxInboundMetadataSize = this.maxInboundMetadataSize;
			copy.userAgent = this.userAgent;
			copy.defaultDeadline = this.defaultDeadline;
			copy.health.copyValuesFrom(this.getHealth());
			copy.secure = this.secure;
			copy.ssl.copyValuesFrom(this.getSsl());
			copy.serviceConfig.putAll(this.serviceConfig);
			return copy;
		}

		/**
		 * Merges this channel configuration with another configuration. Non-null values
		 * in the other configuration take precedence over values in this configuration.
		 * @param other the configuration to merge with (non-null values override this)
		 * @return a new merged configuration
		 */
		ChannelConfig mergeWith(ChannelConfig other) {
			ChannelConfig merged = this.copy();
			merged.address = Optional.ofNullable(other.address).orElse(merged.address);
			merged.defaultDeadline = Optional.ofNullable(other.defaultDeadline).orElse(merged.defaultDeadline);
			merged.defaultLoadBalancingPolicy = Optional.ofNullable(other.defaultLoadBalancingPolicy)
				.orElse(merged.defaultLoadBalancingPolicy);
			merged.enableKeepAlive = Optional.ofNullable(other.enableKeepAlive).orElse(merged.enableKeepAlive);
			merged.idleTimeout = Optional.ofNullable(other.idleTimeout).orElse(merged.idleTimeout);
			merged.keepAliveTime = Optional.ofNullable(other.keepAliveTime).orElse(merged.keepAliveTime);
			merged.keepAliveTimeout = Optional.ofNullable(other.keepAliveTimeout).orElse(merged.keepAliveTimeout);
			merged.keepAliveWithoutCalls = Optional.ofNullable(other.keepAliveWithoutCalls)
				.orElse(merged.keepAliveWithoutCalls);
			merged.maxInboundMessageSize = Optional.ofNullable(other.maxInboundMessageSize)
				.orElse(merged.maxInboundMessageSize);
			merged.maxInboundMetadataSize = Optional.ofNullable(other.maxInboundMetadataSize)
				.orElse(merged.maxInboundMetadataSize);
			merged.negotiationType = Optional.ofNullable(other.negotiationType).orElse(merged.negotiationType);
			merged.secure = Optional.ofNullable(other.secure).orElse(merged.secure);
			merged.userAgent = Optional.ofNullable(other.userAgent).orElse(merged.userAgent);

			merged.health.mergeWith(other.health);
			merged.ssl.mergeWith(other.ssl);

			if (!other.serviceConfig.isEmpty()) {
				merged.serviceConfig.putAll(other.serviceConfig);
			}
			return merged;
		}

		/**
		 * Extracts the service configuration from the client properties, respecting the
		 * yaml lists (e.g. `retryPolicy`).
		 * @return the map for the `serviceConfig` property
		 */
		@SuppressWarnings("NullAway")
		public Map<String, Object> extractServiceConfig() {
			return ConfigurationPropertiesMapUtils.convertIntegerKeyedMapsToLists(getServiceConfig());
		}

		public static class Health {

			/**
			 * Whether to enable client-side health check for the channel.
			 */
			private @Nullable Boolean enabled;

			/**
			 * Name of the service to check health on.
			 */
			private @Nullable String serviceName;

			public boolean isEnabled() {
				return Objects.requireNonNullElse(this.enabled, false);
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public @Nullable String getServiceName() {
				return this.serviceName;
			}

			public void setServiceName(String serviceName) {
				this.serviceName = serviceName;
			}

			/**
			 * Copies the values from another instance.
			 * @param other instance to copy values from
			 */
			void copyValuesFrom(Health other) {
				this.enabled = other.enabled;
				this.serviceName = other.serviceName;
			}

			/**
			 * Merges this health configuration with another. Non-null values in the other
			 * configuration take precedence.
			 * @param other the configuration to merge with
			 */
			void mergeWith(Health other) {
				this.enabled = Optional.ofNullable(other.enabled).orElse(this.enabled);
				this.serviceName = Optional.ofNullable(other.serviceName).orElse(this.serviceName);
			}

		}

		public static class Ssl {

			/**
			 * Whether to enable SSL support. Enabled automatically if "bundle" is
			 * provided unless specified otherwise.
			 */
			private @Nullable Boolean enabled;

			/**
			 * SSL bundle name.
			 */
			private @Nullable String bundle;

			public @Nullable Boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(@Nullable Boolean enabled) {
				this.enabled = enabled;
			}

			public boolean determineEnabled() {
				return (this.enabled != null) ? this.enabled : this.bundle != null;
			}

			public @Nullable String getBundle() {
				return this.bundle;
			}

			public void setBundle(@Nullable String bundle) {
				this.bundle = bundle;
			}

			/**
			 * Copies the values from another instance.
			 * @param other instance to copy values from
			 */
			void copyValuesFrom(Ssl other) {
				this.enabled = other.enabled;
				this.bundle = other.bundle;
			}

			/**
			 * Merges this SSL configuration with another. Non-null values in the other
			 * configuration take precedence.
			 * @param other the configuration to merge with
			 */
			void mergeWith(Ssl other) {
				this.enabled = Optional.ofNullable(other.enabled).orElse(this.enabled);
				this.bundle = Optional.ofNullable(other.bundle).orElse(this.bundle);
			}

		}

	}

}
