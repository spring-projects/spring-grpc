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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import com.google.api.AnnotationsProto;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

/**
 * Protoc plugin that generates Spring MVC controllers for supported HTTP rules.
 *
 * @author Aleksander Brzozowski
 */
public final class TranscodingProtocPlugin {

	private TranscodingProtocPlugin() {
	}

	public static void main(String[] args) throws IOException {
		run(System.in, System.out);
	}

	static void run(InputStream stdin, OutputStream stdout) throws IOException {
		ExtensionRegistry extensions = ExtensionRegistry.newInstance();
		AnnotationsProto.registerAllExtensions(extensions);
		CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(stdin, extensions);
		CodeGeneratorResponse response;
		try {
			response = generate(request);
		}
		catch (GenerationException ex) {
			response = CodeGeneratorResponse.newBuilder().setError(ex.getMessage()).build();
		}
		response.writeTo(stdout);
	}

	private static CodeGeneratorResponse generate(CodeGeneratorRequest request) {
		DescriptorRegistry registry = new DescriptorRegistry(request.getProtoFileList());
		TranscodingModelBuilder modelBuilder = new TranscodingModelBuilder(registry);
		ControllerEmitter emitter = new ControllerEmitter();
		CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();
		Set<String> filesToGenerate = new HashSet<>(request.getFileToGenerateList());

		for (var file : request.getProtoFileList()) {
			if (!filesToGenerate.contains(file.getName())) {
				continue;
			}
			for (TranscodingModel.Service service : modelBuilder.build(file)) {
				String fileName = service.javaPackage().replace('.', '/') + "/" + service.serviceName()
						+ "TranscodingController.java";
				response.addFile(CodeGeneratorResponse.File.newBuilder()
					.setName(fileName)
					.setContent(emitter.emit(service))
					.build());
			}
		}
		return response.build();
	}

}
