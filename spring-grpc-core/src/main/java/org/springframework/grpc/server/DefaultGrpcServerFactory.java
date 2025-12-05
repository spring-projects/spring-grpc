/*
 * Copyright 2024-present the original author or authors.
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

package org.springframework.grpc.server;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.grpc.internal.GrpcUtils;
import org.springframework.grpc.server.service.ServerInterceptorFilter;
import org.springframework.util.StringUtils;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerCredentials;
import io.grpc.ServerInterceptor;
import io.grpc.ServerProvider;
import io.grpc.ServerServiceDefinition;
import io.grpc.TlsServerCredentials;
import io.grpc.TlsServerCredentials.Builder;
import io.grpc.TlsServerCredentials.ClientAuth;

/**
 * Default implementation for {@link GrpcServerFactory gRPC service factories}.
 * <p>
 * The server builder implementation is discovered via Java's SPI mechanism.
 *
 * @param <T> the type of server builder
 * @author David Syer
 * @author Chris Bono
 * @author Andrey Litvitski
 * @see ServerProvider#provider()
 */
public class DefaultGrpcServerFactory<T extends ServerBuilder<T>> implements GrpcServerFactory {

	// VisibleForSubclass
	protected final Log logger = LogFactory.getLog(getClass());

	private final List<ServerServiceDefinition> serviceList = new LinkedList<>();

	private final String address;

	private final List<ServerBuilderCustomizer<T>> serverBuilderCustomizers;

	private @Nullable KeyManagerFactory keyManager;

	private @Nullable TrustManagerFactory trustManager;

	private @Nullable ClientAuth clientAuth;

	private @Nullable ServerServiceDefinitionFilter serviceFilter;

	private @Nullable ServerInterceptorFilter interceptorFilter;

	public DefaultGrpcServerFactory(String address, List<ServerBuilderCustomizer<T>> serverBuilderCustomizers) {
		this.address = address;
		this.serverBuilderCustomizers = Objects.requireNonNull(serverBuilderCustomizers, "serverBuilderCustomizers");
	}

	public DefaultGrpcServerFactory(String address, List<ServerBuilderCustomizer<T>> serverBuilderCustomizers,
			@Nullable KeyManagerFactory keyManager, @Nullable TrustManagerFactory trustManager,
			@Nullable ClientAuth clientAuth) {
		this(address, serverBuilderCustomizers);
		this.keyManager = keyManager;
		this.trustManager = trustManager;
		this.clientAuth = clientAuth;
	}

	public void setServiceFilter(@Nullable ServerServiceDefinitionFilter serviceFilter) {
		this.serviceFilter = serviceFilter;
	}

	public void setInterceptorFilter(@Nullable ServerInterceptorFilter interceptorFilter) {
		this.interceptorFilter = interceptorFilter;
	}

	@Override
	public boolean supports(ServerInterceptor interceptor, ServerServiceDefinition service) {
		return this.interceptorFilter == null || this.interceptorFilter.filter(interceptor, service);
	}

	protected String address() {
		return this.address;
	}

	@Override
	public Server createServer() {
		T builder = newServerBuilder();
		configure(builder, this.serviceList);
		return builder.build();
	}

	@Override
	public void addService(ServerServiceDefinition service) {
		if (this.serviceFilter != null && !this.serviceFilter.filter(service, this)) {
			return;
		}
		this.serviceList.add(service);
	}

	/**
	 * Creates a new server builder.
	 * @return The newly created server builder.
	 */
	@SuppressWarnings("unchecked")
	protected T newServerBuilder() {
		return (T) Grpc.newServerBuilderForPort(port(), credentials());
	}

	/**
	 * Returns the port number on which the server should listen. Use 0 to let the system
	 * choose a port. Use -1 to denote that this server does not listen on a socket.
	 * @return the port number
	 */
	protected int port() {
		return GrpcUtils.getPort(address());
	}

	/**
	 * Get server credentials.
	 * @return some server credentials (default is insecure)
	 */
	protected ServerCredentials credentials() {
		if (this.keyManager == null || port() == -1) {
			return InsecureServerCredentials.create();
		}
		Builder builder = TlsServerCredentials.newBuilder().keyManager(this.keyManager.getKeyManagers());
		if (this.trustManager != null) {
			builder.trustManager(this.trustManager.getTrustManagers());
		}
		if (this.clientAuth != null) {
			builder.clientAuth(this.clientAuth);
		}
		return builder.build();
	}

	/**
	 * Configures the server builder by adding service definitions and applying
	 * customizers to the builder.
	 * <p>
	 * Subclasses can override this to add features that are not yet supported by this
	 * library.
	 * @param builder the server builder to configure
	 * @param serviceDefinitions the service definitions to add to the builder
	 */
	protected void configure(T builder, List<ServerServiceDefinition> serviceDefinitions) {
		configureServices(builder, serviceDefinitions);
		this.serverBuilderCustomizers.forEach((c) -> c.customize(builder));
	}

	/**
	 * Configure the services to be served by the server.
	 * @param builder the server builder to add the services to
	 * @param serviceDefinitions the service definitions to configure and add to the
	 * builder
	 */
	protected void configureServices(T builder, List<ServerServiceDefinition> serviceDefinitions) {
		Set<String> serviceNames = new LinkedHashSet<>();
		serviceDefinitions.forEach((service) -> {
			String serviceName = service.getServiceDescriptor().getName();
			if (!serviceNames.add(serviceName)) {
				throw new IllegalStateException("Found duplicate service implementation: " + serviceName);
			}
			this.logger.info("Registered gRPC service: " + serviceName);
			builder.addService(service);
		});
	}

	protected String hostName() {
		return address().split(":")[0].trim();
	}

	protected SocketAddress socketAddress() {
		String address = address();
		if (address.startsWith("unix:")) {
			throw new UnsupportedOperationException("Unix socket addresses not supported");
		}
		if (address.startsWith("in-process:")) {
			throw new UnsupportedOperationException("In-Process addresses not supported");
		}

		var host = hostName();
		if (StringUtils.hasText(host) && !Objects.equals(host, "*")) {
			return new InetSocketAddress(host, port());
		}
		else {
			return new InetSocketAddress(port());
		}
	}

}
