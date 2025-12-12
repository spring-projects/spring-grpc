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
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GrpcUtilsTests {

	@DisplayName("GetPortShouldReturn")
	@ParameterizedTest(name = "{1} for address {0}") // GH-319
	@MethodSource
	void returnProperPort(String address, int expectedPort) {
		assertThat(GrpcUtils.getPort(address)).isEqualTo(expectedPort);
	}

	static Stream<Arguments> returnProperPort() {
		return Stream.of(arguments("*", GrpcUtils.DEFAULT_PORT), arguments("*:9090", 9090),
				arguments("localhost", GrpcUtils.DEFAULT_PORT), arguments("localhost:9999", 9999),
				arguments("example.com:1234/path", 1234), arguments("example.com/path", GrpcUtils.DEFAULT_PORT),
				arguments("127.0.0.1", GrpcUtils.DEFAULT_PORT), arguments("127.0.0.1:8080", 8080),
				arguments("foo.googleapis.com", GrpcUtils.DEFAULT_PORT), arguments("foo.googleapis.com:8080", 8080),
				arguments("[::1]", GrpcUtils.DEFAULT_PORT), arguments("[::1]:8080", 8080),
				arguments("localhost:xxx", GrpcUtils.DEFAULT_PORT),
				arguments("[2001:db8:85a3:8d3:1319:8a2e:370:7348]", GrpcUtils.DEFAULT_PORT),
				arguments("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443", 443));
	}

	@DisplayName("GetPortShouldReject")
	@ParameterizedTest(name = "address {0} because {1}") // GH-319
	@MethodSource
	void rejectGetPortForInvalidAddress(String address, String startOfExceptionMessage) {
		assertThatIllegalArgumentException().isThrownBy(() -> GrpcUtils.getPort(address))
			.withMessageStartingWith(startOfExceptionMessage);
	}

	static Stream<Arguments> rejectGetPortForInvalidAddress() {
		return Stream.of(arguments("unix:/some/file/somewhere", "Unix domain socket addresses not supported"),
				arguments("tcp://foo:9090", "Addresses with schemas are not supported"),
				arguments("static://foo:9090", "Addresses with schemas are not supported"),
				arguments("http://localhost", "Addresses with schemas are not supported"));
	}

	@DisplayName("GetHostnameShouldReturn")
	@ParameterizedTest(name = "{1} for address {0}") // GH-319
	@MethodSource
	void returnProperHostname(String address, String expectedHostname) {
		assertThat(GrpcUtils.getHostName(address)).isEqualTo(expectedHostname);
	}

	static Stream<Arguments> returnProperHostname() {
		return Stream.of(arguments("*", "*"), arguments("*:9090", "*"), arguments("localhost", "localhost"),
				arguments("localhost:9999", "localhost"), arguments("example.com:1234/path", "example.com"),
				arguments("example.com/path", "example.com"), arguments("127.0.0.1", "127.0.0.1"),
				arguments("127.0.0.1:8080", "127.0.0.1"), arguments("foo.googleapis.com", "foo.googleapis.com"),
				arguments("foo.googleapis.com:8080", "foo.googleapis.com"), arguments("[::1]", "[::1]"),
				arguments("[::1]:8080", "[::1]"),
				arguments("[2001:db8:85a3:8d3:1319:8a2e:370:7348]", "[2001:db8:85a3:8d3:1319:8a2e:370:7348]"),
				arguments("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:443", "[2001:db8:85a3:8d3:1319:8a2e:370:7348]"));
	}

	@DisplayName("GetHostnameShouldReject")
	@ParameterizedTest(name = "address {0} because {1}") // GH-319
	@MethodSource
	void rejectGetHostnameForInvalidAddress(String address, String startOfExceptionMessage) {
		assertThatIllegalArgumentException().isThrownBy(() -> GrpcUtils.getHostName(address))
			.withMessageStartingWith(startOfExceptionMessage);
	}

	static Stream<Arguments> rejectGetHostnameForInvalidAddress() {
		return Stream.of(arguments("unix:/some/file/somewhere", "Unix domain socket addresses not supported"),
				arguments("tcp://foo:9090", "Addresses with schemas are not supported"),
				arguments("static://foo:9090", "Addresses with schemas are not supported"),
				arguments("http://localhost", "Addresses with schemas are not supported"));
	}

}
