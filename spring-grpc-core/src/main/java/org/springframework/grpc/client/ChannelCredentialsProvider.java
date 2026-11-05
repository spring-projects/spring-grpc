/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.client;

import io.grpc.ChannelCredentials;
import io.grpc.InsecureChannelCredentials;

/**
 * A provider for obtaining channel credentials for gRPC client.
 *
 * @author David Syer
 */
public interface ChannelCredentialsProvider {

	/** Insecure credentials provider instance. */
	ChannelCredentialsProvider INSECURE = path -> InsecureChannelCredentials.create();

	ChannelCredentials getChannelCredentials(String path);

}
