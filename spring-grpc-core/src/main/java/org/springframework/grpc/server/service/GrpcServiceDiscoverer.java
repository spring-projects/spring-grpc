/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
	List<GrpcServiceSpec> findServices();

	/**
	 * Find the names of the available gRPC services.
	 * @return list of service names - empty when no services available
	 */
	List<String> listServiceNames();

}
