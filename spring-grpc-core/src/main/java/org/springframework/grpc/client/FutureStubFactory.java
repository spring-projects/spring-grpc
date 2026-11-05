/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
