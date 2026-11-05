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

public final class SingleValueObserver<T> implements StreamObserver<T> {

	private T value;

	private Throwable exception;

	@Override
	public void onNext(T value) {
		this.value = value;
	}

	@Override
	public void onError(Throwable t) {
		this.exception = t;
	}

	@Override
	public void onCompleted() {
		if (this.exception != null) {
			throw new RuntimeException(this.exception);
		}
		if (this.value == null) {
			throw new IllegalStateException("No value received");
		}
	}

	public T getValue() {
		return this.value;
	}

}