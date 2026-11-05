/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import java.lang.reflect.Method;

import org.springframework.util.ReflectionUtils;

import io.grpc.stub.AbstractStub;

public class SimpleStubFactory extends AbstractStubFactory<AbstractStub<?>> {

	public static boolean supports(Class<?> type) {
		Class<?> factory = type.getEnclosingClass();
		return AbstractStubFactory.supports(AbstractStub.class, type) && factory != null && hasMethod(factory, type);
	}

	private static boolean hasMethod(Class<?> factory, Class<?> type) {
		Method method = ReflectionUtils.findMethod(factory, "newStub", (Class<?>[]) null);
		return method != null && method.getReturnType().isAssignableFrom(type);
	}

	@Override
	protected String methodName() {
		return "newStub";
	}

}
