/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.lifecycle;

import java.time.Clock;

import io.grpc.Server;

/**
 * This event will be fired after the server completed to shutdown. The server will no
 * longer process requests.
 *
 * @author Daniel Theuke (daniel.theuke@heuboe.de)
 * @see Server#isTerminated()
 */
public class GrpcServerTerminatedEvent extends GrpcServerLifecycleEvent {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new GrpcServerTerminatedEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param clock The clock used to determine the timestamp.
	 * @param server The server related to this event.
	 */
	public GrpcServerTerminatedEvent(final GrpcServerLifecycle lifecyle, final Clock clock, final Server server) {

		super(lifecyle, clock, server);
	}

	/**
	 * Creates a new GrpcServerTerminatedEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param server The server related to this event.
	 */
	public GrpcServerTerminatedEvent(final GrpcServerLifecycle lifecyle, final Server server) {

		super(lifecyle, server);
	}

}
