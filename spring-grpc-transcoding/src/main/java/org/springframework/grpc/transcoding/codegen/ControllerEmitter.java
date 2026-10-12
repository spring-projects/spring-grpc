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
import java.util.List;

/**
 * Renders a validated transcoding model as a Spring MVC controller.
 *
 * @author Aleksander Brzozowski
 */
final class ControllerEmitter {

	private static final String IMPORTS = """
			import java.util.List;

			import io.grpc.Channel;

			import org.springframework.beans.factory.annotation.Qualifier;
			import org.springframework.http.HttpStatus;
			import org.springframework.web.bind.annotation.DeleteMapping;
			import org.springframework.web.bind.annotation.GetMapping;
			import org.springframework.web.bind.annotation.PathVariable;
			import org.springframework.web.bind.annotation.PostMapping;
			import org.springframework.web.bind.annotation.PutMapping;
			import org.springframework.web.bind.annotation.RequestBody;
			import org.springframework.web.bind.annotation.RequestParam;
			import org.springframework.web.bind.annotation.RestController;
			import org.springframework.web.server.ResponseStatusException;

			""";

	String emit(TranscodingModel.Service service) {
		String controllerName = service.serviceName() + "TranscodingController";
		StringBuilder source = new StringBuilder("// GENERATED CODE - DO NOT EDIT.\n");
		if (!service.javaPackage().isEmpty()) {
			source.append("package ").append(service.javaPackage()).append(";\n\n");
		}
		source.append(IMPORTS)
			.append("@RestController\nclass ")
			.append(controllerName)
			.append(" {\n\n\tprivate final ")
			.append(service.stubType())
			.append('.')
			.append(service.serviceName())
			.append("BlockingStub stub;\n\n\t")
			.append(controllerName)
			.append("(@Qualifier(\"grpcTranscodingChannel\") Channel grpcTranscodingChannel) {\n\t\tthis.stub = ")
			.append(service.stubType())
			.append(".newBlockingStub(grpcTranscodingChannel);\n\t}\n");
		for (TranscodingModel.Method method : service.methods()) {
			emitMethod(source, method);
		}
		return source.append("}\n").toString();
	}

	private void emitMethod(StringBuilder source, TranscodingModel.Method method) {
		source.append("\n\t@")
			.append(mappingAnnotation(method.verb()))
			.append("(\"")
			.append(method.path())
			.append("\")\n\t")
			.append(method.outputType())
			.append(' ')
			.append(method.methodName())
			.append('(')
			.append(String.join(", ", parameterDeclarations(method)))
			.append(") {\n\t\t")
			.append(method.inputType())
			.append(".Builder requestBuilder = ")
			.append(method.body() ? "request.toBuilder()" : method.inputType() + ".newBuilder()")
			.append(";\n");
		for (TranscodingModel.FieldBinding binding : method.queryBindings()) {
			emitQueryBinding(source, binding);
		}
		for (TranscodingModel.FieldBinding binding : method.pathBindings()) {
			source.append("\t\trequestBuilder.")
				.append(binding.setterName())
				.append('(')
				.append(binding.parameterName())
				.append(");\n");
		}
		source.append("\t\treturn this.stub.").append(method.methodName()).append("(requestBuilder.build());\n\t}\n");
	}

	private List<String> parameterDeclarations(TranscodingModel.Method method) {
		List<String> declarations = new ArrayList<>();
		for (TranscodingModel.FieldBinding binding : method.pathBindings()) {
			declarations.add("@PathVariable(\"" + binding.httpName() + "\") String " + binding.parameterName());
		}
		for (TranscodingModel.FieldBinding binding : method.queryBindings()) {
			declarations.add("@RequestParam(name = \"" + binding.httpName() + "\", required = false) List<String> "
					+ binding.parameterName());
		}
		if (method.body()) {
			declarations.add("@RequestBody " + method.inputType() + " request");
		}
		return declarations;
	}

	private void emitQueryBinding(StringBuilder source, TranscodingModel.FieldBinding binding) {
		String parameterName = binding.parameterName();
		source.append("\t\tif (")
			.append(parameterName)
			.append(" != null && ")
			.append(parameterName)
			.append(".size() > 1) {\n\t\t\tthrow new ResponseStatusException(HttpStatus.BAD_REQUEST, ")
			.append("\"Query parameter '")
			.append(binding.httpName())
			.append("' must not occur more than once\");\n\t\t}\n\t\tif (")
			.append(parameterName)
			.append(" != null && !")
			.append(parameterName)
			.append(".isEmpty()) {\n\t\t\trequestBuilder.")
			.append(binding.setterName())
			.append('(')
			.append(parameterName)
			.append(".get(0));\n\t\t}\n");
	}

	private String mappingAnnotation(String verb) {
		return switch (verb) {
			case "GET" -> "GetMapping";
			case "POST" -> "PostMapping";
			case "PUT" -> "PutMapping";
			case "DELETE" -> "DeleteMapping";
			default -> throw new IllegalStateException("Unsupported verb: " + verb);
		};
	}

}
