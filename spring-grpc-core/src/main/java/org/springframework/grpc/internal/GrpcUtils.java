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

/**
 * Provides convenience methods for various gRPC functions.
 *
 * @author David Syer
 */
public final class GrpcUtils {

	private GrpcUtils() {
	}

	/** Default port to use. */
	public static int DEFAULT_PORT = 9090;

	/**
	 * Gets port given an address.
	 * @param address the address to extract port from
	 * @return the port
	 */
	public static int getPort(String address) {
		String value = address;
		if (value.contains(":")) {
			value = value.substring(value.lastIndexOf(":") + 1);
		}
		if (value.contains("/")) {
			value = value.substring(0, value.indexOf("/"));
		}
		if (value.matches("[0-9]+")) {
			return Integer.parseInt(value);
		}
		if (address.startsWith("unix:")) {
			return -1;
		}
		return DEFAULT_PORT;
	}

}
