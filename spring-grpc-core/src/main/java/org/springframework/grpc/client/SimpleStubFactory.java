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

import org.springframework.util.ReflectionUtils;

import io.grpc.stub.AbstractStub;

public class SimpleStubFactory extends AbstractStubFactory<AbstractStub<?>> {

	public static boolean supports(Class<?> type) {
		Class<?> factory = type.getEnclosingClass();
		return AbstractStubFactory.supports(AbstractStub.class, type) && factory != null
				&& ReflectionUtils.findMethod(factory, "newStub", (Class<?>[]) null) != null;
	}

	@Override
	protected String methodName() {
		return "newStub";
	}

}
