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

package org.springframework.grpc.client;

public interface VirtualTargets {

	/**
	 * Default VirtualTargets instance that resolves the logical name "default" to
	 * "localhost:9090" and returns all other targets as-is.
	 */
	VirtualTargets DEFAULT = path -> "default".equals(path) ? "localhost:9090" : path;

	String getTarget(String path);

}
