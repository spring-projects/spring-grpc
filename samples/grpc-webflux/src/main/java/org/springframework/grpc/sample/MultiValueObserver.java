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