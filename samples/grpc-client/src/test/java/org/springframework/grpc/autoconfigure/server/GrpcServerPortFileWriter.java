/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.grpc.autoconfigure.server;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.system.SystemProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.grpc.server.lifecycle.GrpcServerStartedEvent;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * An {@link ApplicationListener} that saves the gRPC server port into a file. This
 * application listener will be triggered whenever the gRPC server starts, and the file
 * name can be overridden at runtime with a System property or environment variable named
 * "PORTFILE" or "portfile".
 *
 * <b>NOTE:</b> This is currently required in order to use spring-boot-testjars as it
 * expects this file to be available in order to determine the port of the dynamically
 * launched gRPC server.
 *
 * @author David Liu
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Dave Syer
 */
public class GrpcServerPortFileWriter implements ApplicationListener<GrpcServerStartedEvent> {

	private static final String DEFAULT_FILE_NAME = "grpc.port";

	private static final String[] PROPERTY_VARIABLES = { "PORTFILE", "portfile" };

	private static final Log logger = LogFactory.getLog(GrpcServerPortFileWriter.class);

	private final File file;

	/**
	 * Create a new {@link GrpcServerPortFileWriter} instance using the filename
	 * 'application.port'.
	 */
	public GrpcServerPortFileWriter() {
		this(new File(DEFAULT_FILE_NAME));
	}

	/**
	 * Create a new {@link GrpcServerPortFileWriter} instance with a specified filename.
	 * @param filename the name of file containing port
	 */
	public GrpcServerPortFileWriter(String filename) {
		this(new File(filename));
	}

	/**
	 * Create a new {@link GrpcServerPortFileWriter} instance with a specified file.
	 * @param file the file containing port
	 */
	public GrpcServerPortFileWriter(File file) {
		Assert.notNull(file, "File must not be null");
		String override = SystemProperties.get(PROPERTY_VARIABLES);
		if (override != null) {
			this.file = new File(override);
		}
		else {
			this.file = file;
		}
	}

	@Override
	public void onApplicationEvent(GrpcServerStartedEvent event) {
		File portFile = this.file;
		try {
			String port = String.valueOf(event.getPort());
			createParentDirectory(portFile);
			FileCopyUtils.copy(port.getBytes(), portFile);
			portFile.deleteOnExit();
		}
		catch (Exception ex) {
			logger.warn(LogMessage.format("Cannot create port file %s", this.file));
		}
	}

	private void createParentDirectory(File file) {
		File parent = file.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}
	}

}
