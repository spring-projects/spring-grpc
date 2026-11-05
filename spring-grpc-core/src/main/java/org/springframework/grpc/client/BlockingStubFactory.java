/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import io.grpc.stub.AbstractBlockingStub;

public class BlockingStubFactory extends AbstractStubFactory<AbstractBlockingStub<?>> {

	public static boolean supports(Class<?> type) {
		return (AbstractStubFactory.supports(AbstractBlockingStub.class, type)
				&& !type.getSimpleName().contains("BlockingV2"))
				// compatible for old grpc version
				|| type.getSimpleName().endsWith("BlockingStub");
	}

	@Override
	protected String methodName() {
		return "newBlockingStub";
	}

}
