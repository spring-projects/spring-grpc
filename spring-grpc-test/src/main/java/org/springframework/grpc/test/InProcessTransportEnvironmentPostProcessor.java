/*
 * Copyright 2024-2024 the original author or authors.
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
package org.springframework.grpc.test;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationAotProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.NativeDetector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

public class InProcessTransportEnvironmentPostProcessor implements EnvironmentPostProcessor {

	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		if (!EnablementDeducer.inTest(Thread.currentThread())) {
			return;
		}
		if (AnnotationUtils.findAnnotation(application.getMainApplicationClass(),
				AutoConfigureInProcessTransport.class) == null) {
			return;
		}
		MapPropertySource inProcessTransportPropertySource = new MapPropertySource("inProcessTransportPropertySource",
				Map.of("spring.grpc.test.inprocess.enabled", "true", "spring.grpc.client.inprocess.exclusive", "true",
						"spring.grpc.server.inprocess.exclusive", "true"));
		environment.getPropertySources().addFirst(inProcessTransportPropertySource);
	}

	/**
	 * Copied from Spring Boot's {@code DevToolsEnablementDeducer}.
	 */
	static final class EnablementDeducer {

		private static final Set<String> INCLUDED_STACK_ELEMENTS;

		static {
			Set<String> included = new LinkedHashSet<>();
			included.add("org.junit.runners.");
			included.add("org.junit.platform.");
			included.add("org.springframework.boot.test.");
			included.add(SpringApplicationAotProcessor.class.getName());
			included.add("cucumber.runtime.");
			INCLUDED_STACK_ELEMENTS = Collections.unmodifiableSet(included);
		}

		private EnablementDeducer() {
		}

		/**
		 * Checks if a specific {@link StackTraceElement} in the current thread's
		 * stacktrace shows that we are being launched from a test, not a main method, and
		 * not in a native image.
		 * @param thread the current thread
		 * @return {@code true} if should be enabled
		 */
		public static boolean inTest(Thread thread) {
			if (NativeDetector.inNativeImage()) {
				return false;
			}
			for (StackTraceElement element : thread.getStackTrace()) {
				if (isIncludedStackElement(element)) {
					return true;
				}
			}
			return false;
		}

		private static boolean isIncludedStackElement(StackTraceElement element) {
			for (String included : INCLUDED_STACK_ELEMENTS) {
				if (element.getClassName().startsWith(included)) {
					return true;
				}
			}
			return false;
		}

	}

}
