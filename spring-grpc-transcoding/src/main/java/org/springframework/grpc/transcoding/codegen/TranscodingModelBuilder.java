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

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;

/**
 * Converts descriptors and HTTP rules into a validated generation model.
 *
 * @author Aleksander Brzozowski
 */
final class TranscodingModelBuilder {

	private final DescriptorRegistry registry;

	private final FieldBindingResolver fieldBindings;

	TranscodingModelBuilder(DescriptorRegistry registry) {
		this.registry = registry;
		this.fieldBindings = new FieldBindingResolver();
	}

	List<TranscodingModel.Service> build(FileDescriptorProto file) {
		List<TranscodingModel.Service> result = new ArrayList<>();
		for (ServiceDescriptorProto service : file.getServiceList()) {
			List<TranscodingModel.Method> methods = new ArrayList<>();
			for (MethodDescriptorProto method : service.getMethodList()) {
				if (!HttpRuleResolver.hasRule(method)) {
					continue;
				}
				try {
					methods.add(buildMethod(file, method));
				}
				catch (GenerationException ex) {
					throw new GenerationException(file.getName() + ": " + service.getName() + "." + method.getName()
							+ ": " + ex.getMessage());
				}
			}
			if (!methods.isEmpty()) {
				String javaPackage = DescriptorRegistry.javaPackage(file);
				String stubType = javaPackage.isEmpty() ? service.getName() + "Grpc"
						: javaPackage + "." + service.getName() + "Grpc";
				result
					.add(new TranscodingModel.Service(javaPackage, service.getName(), stubType, List.copyOf(methods)));
			}
		}
		return List.copyOf(result);
	}

	private TranscodingModel.Method buildMethod(FileDescriptorProto serviceFile, MethodDescriptorProto method) {
		FieldBindingResolver.requireMultipleFiles(serviceFile, "service");
		if (method.getClientStreaming() || method.getServerStreaming()) {
			throw new GenerationException("annotated streaming methods are not supported");
		}

		DescriptorRegistry.MessageType input = this.registry.message(method.getInputType());
		DescriptorRegistry.MessageType output = this.registry.message(method.getOutputType());
		FieldBindingResolver.requireMultipleFiles(input.file(), "request type '" + method.getInputType() + "'");
		FieldBindingResolver.requireMultipleFiles(output.file(), "response type '" + method.getOutputType() + "'");

		HttpRuleResolver.ResolvedRoute route = HttpRuleResolver.resolve(method);
		FieldBindingResolver.Bindings bindings = this.fieldBindings.resolve(input.descriptor(), route.pathVariables(),
				route.body());
		return new TranscodingModel.Method(javaMethodName(method.getName()), input.javaName(), output.javaName(),
				route.verb(), route.path(), route.body(), bindings.path(), bindings.query());
	}

	private String javaMethodName(String protoName) {
		if (protoName.isEmpty()) {
			return protoName;
		}
		return Character.toLowerCase(protoName.charAt(0)) + protoName.substring(1);
	}

}
