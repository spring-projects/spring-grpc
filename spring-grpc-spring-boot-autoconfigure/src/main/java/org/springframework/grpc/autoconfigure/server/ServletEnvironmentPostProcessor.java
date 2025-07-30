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
package org.springframework.grpc.autoconfigure.server;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.ClassUtils;

public class ServletEnvironmentPostProcessor implements EnvironmentPostProcessor {

	private static final boolean SERVLET_AVAILABLE = ClassUtils.isPresent("io.grpc.servlet.jakarta.GrpcServlet", null);

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (SERVLET_AVAILABLE) {
			environment.getPropertySources()
				.addFirst(new MapPropertySource("grpc-servlet", Map.of("server.http2.enabled", "true")));
		}
	}

}
