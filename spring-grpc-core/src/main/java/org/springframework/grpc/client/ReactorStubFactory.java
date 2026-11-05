/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import io.grpc.stub.AbstractStub;

public class ReactorStubFactory extends AbstractStubFactory<AbstractStub<?>> {

	public static boolean supports(Class<?> type) {
		return AbstractStubFactory.supports(AbstractStub.class, type) && type.getSimpleName().startsWith("Reactor");
	}

	@Override
	protected String methodName() {
		return "newReactorStub";
	}

}
