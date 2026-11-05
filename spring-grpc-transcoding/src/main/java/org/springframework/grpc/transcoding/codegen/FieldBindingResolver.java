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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

/**
 * Resolves path and query bindings against a request message descriptor.
 *
 * @author Aleksander Brzozowski
 */
final class FieldBindingResolver {

	private static final Set<String> FORBIDDEN_ACCESSOR_NAMES = Set.of("Class", "DefaultInstanceForType",
			"ParserForType", "SerializedSize", "UnknownFields", "AllFields", "DescriptorForType",
			"InitializationErrorString", "CachedSize");

	Bindings resolve(DescriptorProto request, List<String> pathVariables, boolean hasBody) {
		Map<String, FieldDescriptorProto> fields = new HashMap<>();
		for (FieldDescriptorProto field : request.getFieldList()) {
			fields.put(field.getName(), field);
		}

		Set<String> pathNames = new HashSet<>();
		List<TranscodingModel.FieldBinding> pathBindings = new java.util.ArrayList<>();
		for (int i = 0; i < pathVariables.size(); i++) {
			String variable = pathVariables.get(i);
			FieldDescriptorProto field = fields.get(variable);
			if (field == null) {
				throw new GenerationException("path variable '" + variable + "' does not name a request field");
			}
			validateBindable(field, "path");
			pathNames.add(variable);
			pathBindings.add(binding(field, variable, "pathField" + i));
		}

		List<TranscodingModel.FieldBinding> queryBindings = new java.util.ArrayList<>();
		if (!hasBody) {
			int index = 0;
			Set<String> queryNames = new HashSet<>();
			for (FieldDescriptorProto field : request.getFieldList()) {
				if (!pathNames.contains(field.getName())) {
					validateBindable(field, "query");
					String queryName = jsonName(field);
					if (!queryNames.add(queryName)) {
						throw new GenerationException("multiple query fields use HTTP name '" + queryName + "'");
					}
					queryBindings.add(binding(field, queryName, "queryField" + index++));
				}
			}
		}
		return new Bindings(List.copyOf(pathBindings), List.copyOf(queryBindings));
	}

	private TranscodingModel.FieldBinding binding(FieldDescriptorProto field, String httpName, String parameterName) {
		return new TranscodingModel.FieldBinding(field.getName(), httpName, parameterName,
				"set" + accessorSuffix(field.getName()));
	}

	private void validateBindable(FieldDescriptorProto field, String location) {
		if (field.getLabel() == Label.LABEL_REPEATED) {
			throw new GenerationException(location + " field '" + field.getName() + "' must not be repeated or a map");
		}
		if (field.getType() != Type.TYPE_STRING) {
			throw new GenerationException(location + " field '" + field.getName() + "' must be a string");
		}
	}

	static void requireMultipleFiles(com.google.protobuf.DescriptorProtos.FileDescriptorProto file, String subject) {
		if (!file.hasOptions() || !file.getOptions().getJavaMultipleFiles()) {
			throw new GenerationException(
					subject + " is declared in '" + file.getName() + "', which must set java_multiple_files = true");
		}
	}

	private String jsonName(FieldDescriptorProto field) {
		if (!field.getJsonName().isEmpty()) {
			return field.getJsonName();
		}
		String suffix = accessorSuffix(field.getName());
		return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
	}

	private String accessorSuffix(String protoName) {
		StringBuilder result = new StringBuilder();
		boolean capitalizeNext = true;
		for (int i = 0; i < protoName.length(); i++) {
			char candidate = protoName.charAt(i);
			if (Character.isLetterOrDigit(candidate)) {
				result.append(capitalizeNext ? Character.toUpperCase(candidate) : candidate);
				capitalizeNext = Character.isDigit(candidate);
			}
			else {
				capitalizeNext = true;
			}
		}
		String suffix = result.toString();
		return FORBIDDEN_ACCESSOR_NAMES.contains(suffix) ? suffix + "_" : suffix;
	}

	record Bindings(List<TranscodingModel.FieldBinding> path, List<TranscodingModel.FieldBinding> query) {
	}

}
