/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.lifecycle;

import static java.util.Objects.requireNonNull;

import java.time.Clock;

import org.springframework.context.ApplicationEvent;

import io.grpc.Server;

/**
 * The base event for {@link GrpcServerLifecycle}.
 *
 * @author Michael (yidongnan@gmail.com)
 */
public abstract class GrpcServerLifecycleEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;

	private final Server server;

	/**
	 * Creates a new GrpcServerLifecycleEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param clock The clock used to determine the timestamp.
	 * @param server The server related to this event.
	 */
	protected GrpcServerLifecycleEvent(final GrpcServerLifecycle lifecyle, final Clock clock, final Server server) {
		super(lifecyle, clock);
		this.server = requireNonNull(server, "server must not be null");
	}

	/**
	 * Creates a new GrpcServerLifecycleEvent.
	 * @param lifecyle The lifecycle that caused this event.
	 * @param server The server related to this event.
	 */
	protected GrpcServerLifecycleEvent(final GrpcServerLifecycle lifecyle, final Server server) {
		super(lifecyle);
		this.server = requireNonNull(server, "server must not be null");
	}

	/**
	 * Gets the server related to this event.
	 * @return The server instance.
	 */
	public Server getServer() {
		return this.server;
	}

	/**
	 * Gets the lifecycle instance that triggered this event.
	 */
	@Override
	public GrpcServerLifecycle getSource() {
		return (GrpcServerLifecycle) super.getSource();
	}

}
