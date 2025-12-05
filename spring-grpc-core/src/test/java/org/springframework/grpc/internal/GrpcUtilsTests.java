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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

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

	@TestFactory
	List<DynamicTest> ipAddress() {
		return List.of(testIpAddress(":9999", new InetSocketAddress(9999)),
				testIpAddress("localhost:9999", new InetSocketAddress("localhost", 9999)),
				testIpAddress("localhost", new InetSocketAddress("localhost", 9090)),
				testIpAddress("127.0.0.1", new InetSocketAddress("127.0.0.1", 9090)),
				testIpAddress("127.0.0.1:8888", new InetSocketAddress("127.0.0.1", 8888)),
				testIpAddress("*", new InetSocketAddress(9090)), testIpAddress("*:8888", new InetSocketAddress(8888)),
				testIpAddress("", new InetSocketAddress(9090)),
				// IPv6 cases. See
				// https://en.wikipedia.org/wiki/IPv6#Address_representation
				testIpAddress("[::]:8888", new InetSocketAddress(8888)),
				testIpAddress("::", new InetSocketAddress(9090)), testIpAddress("[::]", new InetSocketAddress(9090)),
				testIpAddress("::1", new InetSocketAddress("::1", 9090)),
				testIpAddress("[::1]", new InetSocketAddress("::1", 9090)),
				testIpAddress("[::1]:9999", new InetSocketAddress("::1", 9999)),
				testIpAddress("2001:db8::ff00:42:8329", new InetSocketAddress("2001:db8::ff00:42:8329", 9090)),
				testIpAddress("[2001:db8::ff00:42:8329]", new InetSocketAddress("2001:db8::ff00:42:8329", 9090)),
				testIpAddress("[2001:db8::ff00:42:8329]:9999", new InetSocketAddress("2001:db8::ff00:42:8329", 9999)),
				testIpAddress("::ffff:192.0.2.128", new InetSocketAddress("::ffff:192.0.2.128", 9090)),
				testIpAddress("[::ffff:192.0.2.128]", new InetSocketAddress("::ffff:192.0.2.128", 9090)),
				testIpAddress("[::ffff:192.0.2.128]:9999", new InetSocketAddress("::ffff:192.0.2.128", 9999)));
	}

	private DynamicTest testIpAddress(String address, SocketAddress expected) {
		return DynamicTest.dynamicTest("Socket address: " + address, () -> {
			assertThat(GrpcUtils.getSocketAddress(address)).isEqualTo(expected);
		});
	}

	@TestFactory
	List<DynamicTest> unsupportedAddress() {
		return List.of(testThrows("unix:dummy", UnsupportedOperationException.class),
				testThrows("0:1:2:3:4:5:6:7:8:9", IllegalArgumentException.class),
				testThrows("[0:1:2:3:4:5:6:7:8:9]", IllegalArgumentException.class),
				testThrows("[0:1:2:3:4:5:6:7:8]:9", IllegalArgumentException.class),
				testThrows("[0:1:2:3:4:5:6:7]:8:9", IllegalArgumentException.class));
	}

	private DynamicTest testThrows(String address, Class<? extends Exception> expectedException) {
		return DynamicTest.dynamicTest("Socket address: " + address, () -> assertThatExceptionOfType(expectedException)
			.isThrownBy(() -> GrpcUtils.getSocketAddress(address)));
	}

}
