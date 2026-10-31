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

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;

/**
 * Resolves and validates the supported {@code google.api.http} subset.
 *
 * @author Aleksander Brzozowski
 */
final class HttpRuleResolver {

	private HttpRuleResolver() {
	}

	static boolean hasRule(MethodDescriptorProto method) {
		return method.hasOptions() && method.getOptions().hasExtension(AnnotationsProto.http);
	}

	static ResolvedRoute resolve(MethodDescriptorProto method) {
		HttpRule rule = method.getOptions().getExtension(AnnotationsProto.http);
		String verb;
		String path;
		switch (rule.getPatternCase()) {
			case GET -> {
				verb = "GET";
				path = rule.getGet();
			}
			case POST -> {
				verb = "POST";
				path = rule.getPost();
			}
			case PUT -> {
				verb = "PUT";
				path = rule.getPut();
			}
			case DELETE -> {
				verb = "DELETE";
				path = rule.getDelete();
			}
			case PATCH -> throw new GenerationException("PATCH is not supported");
			case CUSTOM -> throw new GenerationException("custom HTTP verbs are not supported");
			default -> throw new GenerationException("HTTP rule does not declare a supported path pattern");
		}

		if (!rule.getBody().isEmpty() && !"*".equals(rule.getBody())) {
			throw new GenerationException("named request bodies are not supported: '" + rule.getBody() + "'");
		}
		if (!rule.getResponseBody().isEmpty()) {
			throw new GenerationException("response_body is not supported");
		}
		if (rule.getAdditionalBindingsCount() > 0) {
			throw new GenerationException("additional_bindings are not supported");
		}

		PathTemplate template = PathTemplate.parse(path);
		return new ResolvedRoute(verb, template.getPattern(), template.getVariables(), "*".equals(rule.getBody()));
	}

	record ResolvedRoute(String verb, String path, java.util.List<String> pathVariables, boolean body) {
	}

}
