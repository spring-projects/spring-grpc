/*
 * Copyright 2023-2025 the original author or authors.
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

package org.springframework.grpc.server.service;

import java.util.List;

/**
 * Discovers gRPC services to be provided by the server.
 *
 * @author Michael (yidongnan@gmail.com)
 * @author Chris Bono
 */
public interface GrpcServiceDiscoverer {

	/**
	 * Find the specs of the available gRPC services. The spec can then be passed into a
	 * {@link GrpcServiceConfigurer service configurer} to bind and configure an actual
	 * service definition.
	 * @return list of service specs - empty when no services available
	 */
	List<ServerServiceDefinitionSpec> findServices();

	/**
	 * Find the names of the available gRPC services.
	 * @return list of service names - empty when no services available
	 */
	List<String> listServiceNames();

}
