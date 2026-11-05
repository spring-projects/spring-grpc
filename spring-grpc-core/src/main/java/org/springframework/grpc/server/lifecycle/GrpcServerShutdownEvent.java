/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.lifecycle;

import java.time.Clock;

import io.grpc.Server;

/**
 * This event will be fired before the server starts to shutdown. The server will no
 * longer process new requests.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @see Server#shutdown()
 * @see Server#isShutdown()
 */
public class GrpcServerShutdownEvent extends GrpcServerLifecycleEvent {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new GrpcServerShutdownEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param clock The clock used to determine the timestamp.
	 * @param server The server related to this event.
	 */
	public GrpcServerShutdownEvent(final GrpcServerLifecycle lifecyle, final Clock clock, final Server server) {

		super(lifecyle, clock, server);
	}

	/**
	 * Creates a new GrpcServerShutdownEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param server The server related to this event.
	 */
	public GrpcServerShutdownEvent(final GrpcServerLifecycle lifecyle, final Server server) {

		super(lifecyle, server);
	}

}
