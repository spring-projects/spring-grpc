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

import java.net.URI;
import java.net.URISyntaxException;

import org.jspecify.annotations.Nullable;

/**
 * Provides convenience methods for various gRPC functions.
 * <p>
 * NOTE: Even though this class visibility is `public` it is intended for internal use
 * only and not recommended for direct use.
 *
 * @author David Syer
 * @author Chris Bono
 */
public final class GrpcUtils {

	private GrpcUtils() {
	}

	/**
	 * Server should listen to any IPv4 and IPv6 address.
	 */
	public static final String ANY_IP_ADDRESS = "*";

	/** Default port to use. */
	public static final int DEFAULT_PORT = 9090;

	/**
	 * Gets the port given an address.
	 * <p>
	 * The address is expected to conform to the requirements of the input address for
	 * {@link java.net.URI#URI(String)} with the following exceptions:
	 * <ul>
	 * <li>do not include a scheme
	 * <li>wildcard '*' host is acceptable
	 * </ul>
	 * @param address the server address as described above
	 * @return the port extracted from the address or {@link GrpcUtils#DEFAULT_PORT} if
	 * port was not specified or was invalid (e.g. 'localhost:xxx')
	 * @throws IllegalArgumentException if the address represents a unix domain socket
	 * (i.e. starts with 'unix:') or includes a scheme or {@link URI#URI(String)} is
	 * unable to parse the address
	 */
	public static int getPort(String address) {
		if (address.startsWith("unix:")) {
			throw new IllegalArgumentException("Unix domain socket addresses not supported: " + address);
		}
		if (address.contains("://")) {
			throw new IllegalArgumentException("Addresses with schemas are not supported: " + address);
		}
		// Special case the wildcard as it is not supported by URI.<init>
		if (address.startsWith(ANY_IP_ADDRESS)) {
			address = address.replace(ANY_IP_ADDRESS, "localhost");
		}
		try {
			// Fake schema as it is required by URI.<init>
			int port = new URI("http://" + address).getPort();
			return port == -1 ? DEFAULT_PORT : port;
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(
					"Cannot parse address '%s' due to: %s".formatted(address, ex.getMessage()), ex);
		}
	}

	/**
	 * Gets the hostname given an address.
	 * <p>
	 * The address is expected to conform to the requirements of the input address for
	 * {@link java.net.URI#URI(String)} with the following exceptions:
	 * <ul>
	 * <li>do not include a scheme
	 * <li>wildcard '*' host is acceptable
	 * </ul>
	 * @param address the server address as described above
	 * @return the hostname extracted from the address or null if no hostname could be
	 * extracted
	 * @throws IllegalArgumentException if the address represents a unix domain socket
	 * (i.e. starts with 'unix:') or includes a scheme or {@link URI#URI(String)} is
	 * unable to parse the address
	 */
	public static @Nullable String getHostName(String address) {
		if (address.startsWith("unix:")) {
			throw new IllegalArgumentException("Unix domain socket addresses not supported: " + address);
		}
		if (address.contains("://")) {
			throw new IllegalArgumentException("Addresses with schemas are not supported: " + address);
		}
		if (address.startsWith(ANY_IP_ADDRESS)) {
			return ANY_IP_ADDRESS;
		}
		try {
			// Fake schema as it is required by URI.<init>
			return new URI("http://" + address).getHost();
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(
					"Cannot parse address '%s' due to: %s".formatted(address, ex.getMessage()), ex);
		}
	}

}
