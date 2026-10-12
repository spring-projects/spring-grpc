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

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

/**
 * Index of protobuf descriptors and their generated Java types.
 *
 * @author Aleksander Brzozowski
 */
final class DescriptorRegistry {

	private final Map<String, MessageType> messages = new LinkedHashMap<>();

	DescriptorRegistry(Iterable<FileDescriptorProto> files) {
		for (FileDescriptorProto file : files) {
			String protoPrefix = file.getPackage().isEmpty() ? "" : "." + file.getPackage();
			String javaPackage = javaPackage(file);
			for (DescriptorProto message : file.getMessageTypeList()) {
				indexMessage(file, message, protoPrefix + "." + message.getName(),
						qualify(javaPackage, message.getName()));
			}
		}
	}

	MessageType message(String protoName) {
		MessageType type = this.messages.get(normalize(protoName));
		if (type == null) {
			throw new GenerationException("cannot resolve message type '" + protoName + "'");
		}
		return type;
	}

	static String javaPackage(FileDescriptorProto file) {
		if (file.hasOptions() && !file.getOptions().getJavaPackage().isEmpty()) {
			return file.getOptions().getJavaPackage();
		}
		return file.getPackage();
	}

	private void indexMessage(FileDescriptorProto file, DescriptorProto message, String protoName, String javaName) {
		this.messages.put(protoName, new MessageType(protoName, javaName, file, message));
		for (DescriptorProto nested : message.getNestedTypeList()) {
			indexMessage(file, nested, protoName + "." + nested.getName(), javaName + "." + nested.getName());
		}
	}

	private String normalize(String protoName) {
		return protoName.startsWith(".") ? protoName : "." + protoName;
	}

	private String qualify(String javaPackage, String typeName) {
		return javaPackage.isEmpty() ? typeName : javaPackage + "." + typeName;
	}

	record MessageType(String protoName, String javaName, FileDescriptorProto file, DescriptorProto descriptor) {
	}

}
