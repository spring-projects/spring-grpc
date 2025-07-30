/*
 * Copyright 2016-present the original author or authors.
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
 *
 * Partial copy from net.devh:grpc-spring-boot-starter.
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
