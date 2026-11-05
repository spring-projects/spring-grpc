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

package org.springframework.grpc.transcoding.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validated representation of the simple path-template subset.
 *
 * @author Aleksander Brzozowski
 */
final class PathTemplate {

	private static final Pattern VARIABLE = Pattern.compile("\\{([A-Za-z_][A-Za-z0-9_]*)}");

	private static final Pattern LITERAL = Pattern.compile("[A-Za-z0-9._~-]+");

	private final String pattern;

	private final List<String> variables;

	private PathTemplate(String pattern, List<String> variables) {
		this.pattern = pattern;
		this.variables = variables;
	}

	String getPattern() {
		return this.pattern;
	}

	List<String> getVariables() {
		return this.variables;
	}

	static PathTemplate parse(String template) {
		if (template == null || !template.startsWith("/")) {
			throw new GenerationException("HTTP path must start with '/'");
		}
		if (template.length() > 1 && template.endsWith("/")) {
			throw new GenerationException("HTTP path must not have a trailing '/'");
		}

		List<String> variables = new ArrayList<>();
		Set<String> uniqueVariables = new HashSet<>();
		String[] segments = template.substring(1).split("/", -1);
		for (String segment : segments) {
			if (segment.isEmpty()) {
				throw new GenerationException("HTTP path must not contain empty segments");
			}
			var matcher = VARIABLE.matcher(segment);
			if (matcher.matches()) {
				String variable = matcher.group(1);
				if (!uniqueVariables.add(variable)) {
					throw new GenerationException("duplicate path variable '" + variable + "'");
				}
				variables.add(variable);
			}
			else if (!LITERAL.matcher(segment).matches()) {
				throw new GenerationException("unsupported HTTP path segment '" + segment + "'");
			}
		}
		return new PathTemplate(template, List.copyOf(variables));
	}

}
