/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.function.Supplier;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.kotlin.AbstractCoroutineStub;

public class CoroutineStubFactory implements StubFactory<AbstractCoroutineStub<?>> {

	@Override
	public AbstractCoroutineStub<?> create(Supplier<ManagedChannel> channel,
			Class<? extends AbstractCoroutineStub<?>> type) {
		try {
			return type.getConstructor(Channel.class, CallOptions.class)
				.newInstance(channel.get(), CallOptions.DEFAULT);

		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to create stub", e);
		}
	}

	protected static boolean supports(Class<?> type) {
		return AbstractCoroutineStub.class.isAssignableFrom(type);
	}

}
