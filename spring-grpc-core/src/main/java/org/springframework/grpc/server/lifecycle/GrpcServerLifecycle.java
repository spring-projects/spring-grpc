/*
 * Copyright 2016-2024 the original author or authors.
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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.grpc.server.GrpcServerFactory;

import io.grpc.Server;

/**
 * Lifecycle bean that automatically starts and stops the grpc server.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Dave Syer
 */
public class GrpcServerLifecycle implements SmartLifecycle {

	private static final Log logger = LogFactory.getLog(GrpcServerLifecycle.class);

	private static final AtomicInteger serverCounter = new AtomicInteger(-1);

	private final GrpcServerFactory factory;

	private final Duration shutdownGracePeriod;

	private final ApplicationEventPublisher eventPublisher;

	private Server server;

	/**
	 * Creates a new GrpcServerLifecycle.
	 * @param factory The server factory to use.
	 * @param shutdownGracePeriod The time to wait for the server to gracefully shut down.
	 * @param eventPublisher The event publisher to use.
	 */
	public GrpcServerLifecycle(GrpcServerFactory factory, Duration shutdownGracePeriod,
			ApplicationEventPublisher eventPublisher) {
		this.factory = requireNonNull(factory, "factory");
		this.shutdownGracePeriod = requireNonNull(shutdownGracePeriod, "shutdownGracePeriod");
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void start() {
		try {
			createAndStartGrpcServer();
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to start the grpc server", e);
		}
	}

	@Override
	public void stop() {
		stopAndReleaseGrpcServer();
	}

	@Override
	public void stop(final Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public boolean isRunning() {
		return this.server != null && !this.server.isShutdown();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	public int getPort() {
		return this.server == null ? 0 : this.server.getPort();
	}

	/**
	 * Gets the server factory used to create the server.
	 * @return the server factory to create the server
	 */
	public GrpcServerFactory getFactory() {
		return this.factory;
	}

	/**
	 * Creates and starts the grpc server.
	 * @throws IOException If the server is unable to bind the port.
	 */
	protected void createAndStartGrpcServer() throws IOException {
		if (this.server == null) {
			final Server localServer = this.factory.createServer();
			if (localServer != null) {

				this.server = localServer.start();
				final String address = this.server.getListenSockets().toString();
				final int port = this.server.getPort();
				logger.info("gRPC Server started, listening on address: " + this.server.getListenSockets());
				this.eventPublisher.publishEvent(new GrpcServerStartedEvent(this, localServer, address, port));

				// Prevent the JVM from shutting down while the server is running
				final Thread awaitThread = new Thread(() -> {
					try {
						localServer.awaitTermination();
					}
					catch (final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
				awaitThread.setName("grpc-server-container-" + (serverCounter.incrementAndGet()));
				awaitThread.setDaemon(false);
				awaitThread.start();

			}
		}
	}

	/**
	 * Initiates an orderly shutdown of the grpc server and releases the references to the
	 * server. This call waits for the server to be completely shut down.
	 */
	protected void stopAndReleaseGrpcServer() {
		final Server localServer = this.server;
		if (localServer != null) {
			final long millis = this.shutdownGracePeriod.toMillis();
			logger.debug("Initiating gRPC server shutdown");
			this.eventPublisher.publishEvent(new GrpcServerShutdownEvent(this, localServer));
			localServer.shutdown();
			// Wait for the server to shutdown completely before continuing with
			// destroying
			// the spring context
			try {
				if (millis > 0) {
					localServer.awaitTermination(millis, TimeUnit.MILLISECONDS);
				}
				else if (millis == 0) {
					// Do not wait
				}
				else {
					// Wait infinitely
					localServer.awaitTermination();
				}
			}
			catch (final InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			finally {
				localServer.shutdownNow();
				this.server = null;
			}
			logger.info("Completed gRPC server shutdown");
			this.eventPublisher.publishEvent(new GrpcServerTerminatedEvent(this, localServer));
		}
	}

}
