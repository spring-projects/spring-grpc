/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import io.grpc.ChannelCredentials;
import io.grpc.ClientInterceptor;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;

/**
 * Factory interface for creating {@link ManagedChannel} instances.
 *
 * @author Dave Syer
 * @author Chris Bono
 */
public interface GrpcChannelFactory {

	/**
	 * Whether this factory supports the given target string.
	 * <p>
	 * The target can be either a valid nameresolver-compliant URI or an authority string
	 * as described in {@link Grpc#newChannelBuilder(String, ChannelCredentials)}, or the
	 * name of a user-configured channel (e.g. 'my-channel').
	 * @param target the target string
	 * @return whether this factory supports the given target string
	 */
	boolean supports(String target);

	/**
	 * Whether this factory supports the given global {@link ClientInterceptor}.
	 * @param interceptor the client interceptor to check support for
	 * @return whether this factory supports the given interceptor
	 */
	default boolean supports(ClientInterceptor interceptor) {
		return true;
	}

	/**
	 * Creates a {@link ManagedChannel} for the given target string.
	 * <p>
	 * The target can be either a valid nameresolver-compliant URI or an authority string
	 * as described in {@link Grpc#newChannelBuilder(String, ChannelCredentials)}, or the
	 * name of a user-configured channel (e.g. 'my-channel').
	 * <p>
	 * The returned channel is configured to use all globally registered
	 * {@link ClientInterceptor interceptors}.
	 * @param target the target string
	 * @return a channel for the given target
	 */
	default ManagedChannel createChannel(String target) {
		return this.createChannel(target, ChannelBuilderOptions.defaults());
	}

	/**
	 * Creates a {@link ManagedChannel} for the given target string. The target can be
	 * <p>
	 * The target can be either a valid nameresolver-compliant URI or an authority string
	 * as described in {@link Grpc#newChannelBuilder(String, ChannelCredentials)}, or the
	 * name of a user-configured channel (e.g. 'my-channel').
	 * <p>
	 * The returned channel is configured to use all globally registered
	 * {@link ClientInterceptor interceptors} and any user-provided interceptor if
	 * specified in the {@link ChannelBuilderOptions#customizer() options}.
	 * @param target the target string as described in method javadocs
	 * @param options the options for building the created channel, or
	 * {@link ChannelBuilderOptions#defaults()} to use default options
	 * @return a channel for the given target
	 */
	ManagedChannel createChannel(String target, ChannelBuilderOptions options);

}
