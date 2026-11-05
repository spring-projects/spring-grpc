/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultGrpcChannelFactory}.
 *
 * @author Andrey Litvitski
 */
class DefaultGrpcChannelFactoryTests {

	@Test
	void defaultTargetResolvesToLocalhost() {
		var factory = new DefaultGrpcChannelFactory<>(List.of(), mock());
		assertThat(factory.targets).isSameAs(VirtualTargets.DEFAULT);
		assertThat(factory.targets.getTarget("default")).isEqualTo("localhost:9090");
	}

}
