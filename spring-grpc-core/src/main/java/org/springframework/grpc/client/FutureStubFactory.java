/*
 * Copyright 2024-2024 the original author or authors.
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

import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.AbstractFutureStub;

public class FutureStubFactory extends AbstractStubFactory<AbstractBlockingStub<?>> {

	public static boolean supports(Class<?> type) {
		return AbstractStubFactory.supports(AbstractFutureStub.class, type);
	}

	@Override
	protected String methodName() {
		return "newFutureStub";
	}

}
