/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

/**
 * Identifies the negotiation used for starting up HTTP/2.
 *
 * @author David Syer
 * @see io.grpc.netty.NegotiationType NegotiationType
 */
public enum NegotiationType {

	/**
	 * Uses TLS ALPN/NPN negotiation, assumes an SSL connection.
	 */
	TLS,

	/**
	 * Use the HTTP UPGRADE protocol for a plaintext (non-SSL) upgrade from HTTP/1.1 to
	 * HTTP/2.
	 */
	PLAINTEXT_UPGRADE,

	/**
	 * Just assume the connection is plaintext (non-SSL) and the remote endpoint supports
	 * HTTP/2 directly without an upgrade.
	 */
	PLAINTEXT;

}
