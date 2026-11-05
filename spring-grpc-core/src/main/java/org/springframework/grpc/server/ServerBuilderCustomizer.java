/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server;

import io.grpc.ServerBuilder;

/**
 * Callback interface that can be used to customize a {@link ServerBuilder}.
 *
 * @param <T> the type of server builder
 * @author Chris Bono
 */
@FunctionalInterface
public interface ServerBuilderCustomizer<T extends ServerBuilder<T>> {

	/**
	 * Callback to customize a {@link ServerBuilder} instance.
	 * @param serverBuilder the builder to customize
	 */
	void customize(T serverBuilder);

}
