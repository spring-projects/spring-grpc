/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.sample;

import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public final class MultiValueObserver<T> implements StreamObserver<T> {

	private Sinks.Many<T> sink = Sinks.many().unicast().onBackpressureBuffer();

	@Override
	public void onNext(T value) {
		this.sink.tryEmitNext(value);
	}

	@Override
	public void onError(Throwable t) {
		this.sink.tryEmitError(t);
	}

	@Override
	public void onCompleted() {
		this.sink.tryEmitComplete();
	}

	public Flux<T> getValue() {
		return this.sink.asFlux();
	}

}