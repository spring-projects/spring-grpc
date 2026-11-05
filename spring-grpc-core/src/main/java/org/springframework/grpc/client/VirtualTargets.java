/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

public interface VirtualTargets {

	/**
	 * Default VirtualTargets instance that resolves the logical name "default" to
	 * "localhost:9090" and returns all other targets as-is.
	 */
	VirtualTargets DEFAULT = path -> "default".equals(path) ? "localhost:9090" : path;

	String getTarget(String path);

}
