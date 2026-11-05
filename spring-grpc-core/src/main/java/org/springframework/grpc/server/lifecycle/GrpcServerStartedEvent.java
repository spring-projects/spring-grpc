/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.lifecycle;

import static java.util.Objects.requireNonNull;

import java.time.Clock;

import io.grpc.Server;

/**
 * This event will be fired after the server has been started.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @see Server#start()
 */
public class GrpcServerStartedEvent extends GrpcServerLifecycleEvent {

	private static final long serialVersionUID = 1L;

	private final String address;

	private final int port;

	/**
	 * Creates a new GrpcServerStartedEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param clock The clock used to determine the timestamp.
	 * @param server The server related to this event.
	 * @param address The address the server is bound to.
	 * @param port The port the server is bound to or {@code -1} if it isn't bound to a
	 * particular port.
	 */
	public GrpcServerStartedEvent(final GrpcServerLifecycle lifecyle, final Clock clock, final Server server,
			final String address, final int port) {

		super(lifecyle, clock, server);
		this.address = requireNonNull(address, "address must not be null");
		this.port = port;
	}

	/**
	 * Creates a new GrpcServerStartedEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param server The server related to this event.
	 * @param address The address the server is bound to.
	 * @param port The port the server is bound to or {@code -1} if it isn't bound to a
	 * particular port.
	 */
	public GrpcServerStartedEvent(final GrpcServerLifecycle lifecyle, final Server server, final String address,
			final int port) {

		super(lifecyle, server);
		this.address = requireNonNull(address, "address must not be null");
		this.port = port;
	}

	/**
	 * Gets the address the server was started with.
	 * @return The address to use.
	 */
	public String getAddress() {
		return this.address;
	}

	/**
	 * Gets the main port the server uses.
	 * @return The main port of the server. {@code -1} indicates that the server isn't
	 * bound to a particular port.
	 */
	public int getPort() {
		return this.port;
	}

}
