/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.util.function.Supplier;

import io.grpc.ManagedChannel;

public interface StubFactory<T> {

	T create(Supplier<ManagedChannel> channel, Class<? extends T> type);

}
