/*
 * Copyright 2025-current the original author or authors.
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