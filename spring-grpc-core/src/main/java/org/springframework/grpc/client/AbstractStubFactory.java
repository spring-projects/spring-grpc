/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.function.Supplier;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.stub.AbstractStub;

public abstract class AbstractStubFactory<T extends AbstractStub<?>> implements StubFactory<T> {

	protected static <S extends AbstractStub<?>> boolean supports(Class<S> baseType, Class<?> type) {
		return baseType.isAssignableFrom(type);
	}

	@Override
	public T create(Supplier<ManagedChannel> channel, Class<? extends T> type) {
		// All the generated stubs are static inner classes of the service
		Class<?> factory = type.getEnclosingClass();
		@SuppressWarnings("unchecked")
		T stub = (T) createStub(channel, factory, methodName());
		return stub;
	}

	private Object createStub(Supplier<ManagedChannel> channel, Class<?> factory, String method) {
		try {
			return factory.getMethod(method, Channel.class).invoke(null, channel.get());
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to create stub", e);
		}
	}

	protected abstract String methodName();

}
