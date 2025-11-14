/*
 * Copyright 2023-present the original author or authors.
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

package org.springframework.grpc.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GrpcUtilsTests {

	@Test
	void testGetPortFromAddress() {
		assertThat(GrpcUtils.getPort("localhost:8080")).isEqualTo(8080);
	}

	@Test
	void testGetNoPort() {
		assertThat(GrpcUtils.getPort("localhost")).isEqualTo(9090);
	}

	@Test
	void testGetPortFromAddressWithPath() {
		String address = "example.com:1234/path";
		assertThat(GrpcUtils.getPort(address)).isEqualTo(1234);
	}

	@Test
	void testGetDomainAddress() {
		String address = "unix:/some/file/somewhere";
		assertThat(GrpcUtils.getPort(address)).isEqualTo(-1);
	}

	@Test
	void testGetStaticSchema() {
		String address = "static://localhost";
		assertThat(GrpcUtils.getPort(address)).isEqualTo(9090);
	}

	@Test
	void testGetInvalidAddress() {
		String address = "invalid:broken";
		assertThat(GrpcUtils.getPort(address)).isEqualTo(9090); // -1?
	}

}
