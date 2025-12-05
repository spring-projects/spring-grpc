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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

import org.springframework.util.StringUtils;

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
		long numberOfColons = countColons(address);
		if (numberOfColons == 1) {
			value = value.substring(value.lastIndexOf(":") + 1);
		}
		else if (numberOfColons > 1) {
			if (address.startsWith("[")) {
				var index = address.lastIndexOf("]:");
				if (index >= 0) {
					value = address.substring(index + 2);
				}
				else {
					return DEFAULT_PORT;
				}
			}
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

	public static long countColons(String address) {
		return address.chars().filter(ch -> ch == ':').count();
	}

	/**
	 * Gets the hostname from a given address.
	 * @param address a hostname/IPv4/IPv6/empty/* optionally with a port specification
	 * @return the hostname or an empty string
	 * @see <a href=
	 * "https://en.wikipedia.org/wiki/IPv6#Address_representation">https://en.wikipedia.org/wiki/IPv6#Address_representation</a>
	 */
	public static String getHostName(String address) {
		String trimmedAddress = address.trim();
		long numberOfColons = countColons(trimmedAddress);

		if (numberOfColons == 0) {
			return trimmedAddress;
		}

		if (numberOfColons == 1) { // An IPv6 address mush have at least 2 colons, so is
									// {IPv4 or hostname}:{port}
			return trimmedAddress.split(":")[0].trim();
		}

		if (numberOfColons > 8 || numberOfColons == 8 && !trimmedAddress.startsWith("[")) {
			// On an IPv6 address a maximum of 7 colons are allowed + 1 for the port
			// IPv6 addresses with port should have the format [{address}]:port
			throw new IllegalArgumentException("Cannot parse address: " + trimmedAddress);
		}

		if (trimmedAddress.startsWith("[")) {
			var index = trimmedAddress.lastIndexOf("]");
			if (index < 0) {
				throw new IllegalArgumentException("Cannot parse address: " + trimmedAddress);
			}
			return trimmedAddress.substring(1, index);
		}

		return trimmedAddress; // IPv6 Address with no port specified
	}

	/**
	 * Gets a SocketAddress for the given address.
	 *
	 * If the address part is empty, * or :: a SocketAddress with wildcard address will be
	 * returned. If the port part is empty or missing, a SocketAddress for the gRPC
	 * default port (9090) will be returned.
	 * @param address a hostname/IPv4/IPv6/empty/* optionally with a port specification
	 * @return a SocketAddress representation for the given address
	 */
	public static SocketAddress getSocketAddress(String address) {
		if (address.startsWith("unix:")) {
			throw new UnsupportedOperationException("Unix socket addresses not supported");
		}

		var host = getHostName(address);
		if (StringUtils.hasText(host) && !Objects.equals(host, "*") && !Objects.equals(host, "::")) {
			return new InetSocketAddress(host, getPort(address));
		}
		else {
			return new InetSocketAddress(getPort(address));
		}
	}

}
