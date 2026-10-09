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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileOptions;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.MethodOptions;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

/**
 * End-to-end tests for {@link TranscodingProtocPlugin}.
 *
 * @author Aleksander Brzozowski
 */
class TranscodingProtocPluginTests {

	@TempDir
	Path temporaryDirectory;

	@Test
	void controllerWithImportedNestedRequestAndStringQueryCompiles() throws IOException {
		FileDescriptorProto types = importedTypesFile();
		FileDescriptorProto service = serviceFile("GET", HttpRule.newBuilder().setGet("/v1/fetch").build(), true,
				false);

		CodeGeneratorResponse response = runPlugin(request(service, types));
		CodeGeneratorResponse.File generated = onlyGeneratedFile(response);

		String source = generated.getContent();
		assertThat(source).contains("types.api.Container.Nested.Builder requestBuilder")
			.contains("@RequestParam(name = \"queryText\", required = false) List<String> queryField0")
			.contains("requestBuilder.setQueryText(queryField0.get(0))")
			.contains("@Qualifier(\"grpcTranscodingChannel\")");
		assertCompiles(generated);
	}

	@Test
	void bodyIsDecodedBeforePathBinding() throws IOException {
		FileDescriptorProto service = localServiceFile(
				HttpRule.newBuilder().setPost("/v1/items/{id}").setBody("*").build(), false, true);

		CodeGeneratorResponse response = runPlugin(request(service));

		assertThat(response.getError()).isEmpty();
		assertThat(response.getFile(0).getContent()).contains(".Builder requestBuilder = request.toBuilder()")
			.contains("requestBuilder.setId(pathField0)")
			.doesNotContain("@RequestParam");
	}

	@Test
	void duplicateQueryValuesAreRejected() throws IOException {
		CodeGeneratorResponse response = runPlugin(
				request(serviceFile("GET", HttpRule.newBuilder().setGet("/v1/fetch").build(), true, false),
						importedTypesFile()));

		assertThat(response.getFile(0).getContent()).contains("if (queryField0 != null && queryField0.size() > 1)")
			.contains("Query parameter 'queryText' must not occur more than once");
	}

	@Test
	void unsupportedAnnotatedRuleFailsGenerationWithoutPartialFiles() throws IOException {
		FileDescriptorProto supported = localServiceFile(HttpRule.newBuilder().setGet("/v1/items/{id}").build(), false,
				true);
		FileDescriptorProto unsupported = localServiceFile(HttpRule.newBuilder().setPatch("/v1/items/{id}").build(),
				false, true)
			.toBuilder()
			.setName("unsupported.proto")
			.build();

		CodeGeneratorResponse response = runPlugin(CodeGeneratorRequest.newBuilder()
			.addFileToGenerate(supported.getName())
			.addFileToGenerate(unsupported.getName())
			.addProtoFile(supported)
			.addProtoFile(unsupported)
			.build());

		assertThat(response.getFileCount()).isZero();
		assertThat(response.getError()).contains("unsupported.proto: Items.UpdateItem: PATCH is not supported");
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			GET    | @GetMapping
			POST   | @PostMapping
			PUT    | @PutMapping
			DELETE | @DeleteMapping
			""")
	void eachSupportedVerbGeneratesItsSpringMapping(String verb, String expectedMapping) throws IOException {
		CodeGeneratorResponse.File generated = onlyGeneratedFile(
				runPlugin(request(localServiceFile(httpRule(verb), false, true))));

		assertThat(generated.getContent()).contains(expectedMapping);
	}

	@ParameterizedTest(name = "{0}")
	@CsvSource(delimiter = '|', textBlock = """
			named body            | named request bodies are not supported
			response body         | response_body is not supported
			additional binding    | additional_bindings are not supported
			unknown path variable | does not name a request field
			""")
	void unsupportedHttpRuleFeaturesProduceContextualErrors(String feature, String expectedError) throws IOException {
		CodeGeneratorResponse response = runPlugin(
				request(localServiceFile(unsupportedHttpRule(feature), false, true)));

		assertThat(response.getFileCount()).isZero();
		assertThat(response.getError()).contains("items.proto: Items.UpdateItem:", expectedError);
	}

	@Test
	void unsupportedQueryFieldsFailGeneration() throws IOException {
		HttpRule get = HttpRule.newBuilder().setGet("/v1/items/{id}").build();
		FileDescriptorProto base = localServiceFile(get, false, true);
		DescriptorProto repeatedRequest = base.getMessageType(0)
			.toBuilder()
			.addField(field("tags", 2, Type.TYPE_STRING).toBuilder().setLabel(Label.LABEL_REPEATED))
			.build();
		CodeGeneratorResponse repeatedResponse = runPlugin(
				request(base.toBuilder().setMessageType(0, repeatedRequest).build()));
		assertThat(repeatedResponse.getError()).contains("query field 'tags' must not be repeated or a map");

		DescriptorProto messageRequest = base.getMessageType(0)
			.toBuilder()
			.addField(field("child", 2, Type.TYPE_MESSAGE).toBuilder().setTypeName(".items.ItemReply"))
			.build();
		CodeGeneratorResponse messageResponse = runPlugin(
				request(base.toBuilder().setMessageType(0, messageRequest).build()));
		assertThat(messageResponse.getError()).contains("query field 'child' must be a string");

		DescriptorProto numericRequest = base.getMessageType(0)
			.toBuilder()
			.addField(field("page_size", 2, Type.TYPE_INT32))
			.build();
		CodeGeneratorResponse numericResponse = runPlugin(
				request(base.toBuilder().setMessageType(0, numericRequest).build()));
		assertThat(numericResponse.getError()).contains("query field 'page_size' must be a string");
	}

	@Test
	void nonStringPathFieldFailsGeneration() throws IOException {
		FileDescriptorProto base = localServiceFile(HttpRule.newBuilder().setGet("/v1/items/{id}").build(), false,
				true);
		DescriptorProto numericRequest = base.getMessageType(0)
			.toBuilder()
			.setField(0, field("id", 1, Type.TYPE_INT64))
			.build();

		CodeGeneratorResponse response = runPlugin(request(base.toBuilder().setMessageType(0, numericRequest).build()));

		assertThat(response.getError()).contains("path field 'id' must be a string");
	}

	@Test
	void outerClassLayoutFailsGeneration() throws IOException {
		FileDescriptorProto service = localServiceFile(HttpRule.newBuilder().setGet("/v1/items/{id}").build(), false,
				false);

		CodeGeneratorResponse response = runPlugin(request(service));

		assertThat(response.getFileCount()).isZero();
		assertThat(response.getError()).contains("Items.UpdateItem").contains("must set java_multiple_files = true");
	}

	@Test
	void annotatedStreamingMethodFailsGeneration() throws IOException {
		FileDescriptorProto service = localServiceFile(HttpRule.newBuilder().setGet("/v1/items/{id}").build(), true,
				true);

		CodeGeneratorResponse response = runPlugin(request(service));

		assertThat(response.getError()).contains("annotated streaming methods are not supported");
	}

	@Test
	void unannotatedStreamingMethodIsIgnored() throws IOException {
		FileDescriptorProto service = localServiceFile(null, true, true);

		CodeGeneratorResponse response = runPlugin(request(service));

		assertThat(response.getError()).isEmpty();
		assertThat(response.getFileCount()).isZero();
	}

	private FileDescriptorProto importedTypesFile() {
		DescriptorProto nested = DescriptorProto.newBuilder()
			.setName("Nested")
			.addField(field("query_text", 1, Type.TYPE_STRING).toBuilder().setJsonName("queryText"))
			.build();
		DescriptorProto container = DescriptorProto.newBuilder().setName("Container").addNestedType(nested).build();
		return FileDescriptorProto.newBuilder()
			.setName("types.proto")
			.setPackage("types")
			.setOptions(options("types.api", true))
			.addMessageType(container)
			.build();
	}

	private FileDescriptorProto serviceFile(String verb, HttpRule rule, boolean importedInput, boolean streaming) {
		MethodDescriptorProto.Builder method = MethodDescriptorProto.newBuilder()
			.setName("Fetch")
			.setInputType(importedInput ? ".types.Container.Nested" : ".service.Request")
			.setOutputType(".service.Reply")
			.setServerStreaming(streaming);
		if (rule != null) {
			method.setOptions(MethodOptions.newBuilder().setExtension(AnnotationsProto.http, rule));
		}
		return FileDescriptorProto.newBuilder()
			.setName("service.proto")
			.setPackage("service")
			.setOptions(options("service.api", true))
			.addDependency(importedInput ? "types.proto" : "")
			.addMessageType(DescriptorProto.newBuilder().setName("Reply"))
			.addService(ServiceDescriptorProto.newBuilder().setName("Test").addMethod(method))
			.build();
	}

	private FileDescriptorProto localServiceFile(HttpRule rule, boolean streaming, boolean multipleFiles) {
		MethodDescriptorProto.Builder method = MethodDescriptorProto.newBuilder()
			.setName("UpdateItem")
			.setInputType(".items.ItemRequest")
			.setOutputType(".items.ItemReply")
			.setServerStreaming(streaming);
		if (rule != null) {
			method.setOptions(MethodOptions.newBuilder().setExtension(AnnotationsProto.http, rule));
		}
		return FileDescriptorProto.newBuilder()
			.setName("items.proto")
			.setPackage("items")
			.setOptions(options("items.api", multipleFiles))
			.addMessageType(
					DescriptorProto.newBuilder().setName("ItemRequest").addField(field("id", 1, Type.TYPE_STRING)))
			.addMessageType(DescriptorProto.newBuilder().setName("ItemReply"))
			.addService(ServiceDescriptorProto.newBuilder().setName("Items").addMethod(method))
			.build();
	}

	private FieldDescriptorProto field(String name, int number, Type type) {
		return FieldDescriptorProto.newBuilder()
			.setName(name)
			.setNumber(number)
			.setLabel(Label.LABEL_OPTIONAL)
			.setType(type)
			.build();
	}

	private FileOptions options(String javaPackage, boolean multipleFiles) {
		return FileOptions.newBuilder().setJavaPackage(javaPackage).setJavaMultipleFiles(multipleFiles).build();
	}

	private CodeGeneratorRequest request(FileDescriptorProto generated, FileDescriptorProto... dependencies) {
		CodeGeneratorRequest.Builder request = CodeGeneratorRequest.newBuilder()
			.addFileToGenerate(generated.getName())
			.addProtoFile(generated);
		for (FileDescriptorProto dependency : dependencies) {
			request.addProtoFile(dependency);
		}
		return request.build();
	}

	private CodeGeneratorResponse runPlugin(CodeGeneratorRequest request) throws IOException {
		ByteArrayOutputStream stdout = new ByteArrayOutputStream();
		TranscodingProtocPlugin.run(new ByteArrayInputStream(request.toByteArray()), stdout);
		return CodeGeneratorResponse.parseFrom(stdout.toByteArray());
	}

	private CodeGeneratorResponse.File onlyGeneratedFile(CodeGeneratorResponse response) {
		assertThat(response.getError()).isEmpty();
		assertThat(response.getFileCount()).isOne();
		return response.getFile(0);
	}

	private void assertCompiles(CodeGeneratorResponse.File generated) throws IOException {
		List<Path> sources = new ArrayList<>();
		sources.add(writeSource(generated.getName(), generated.getContent()));
		sources.add(writeSource("types/api/Container.java", containerStub()));
		sources.add(writeSource("service/api/Reply.java", replyStub()));
		sources.add(writeSource("service/api/TestGrpc.java", grpcStub()));

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
			Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(sources);
			Path output = Files.createDirectories(this.temporaryDirectory.resolve("classes"));
			boolean compiled = compiler.getTask(null, fileManager, diagnostics,
					List.of("-classpath", System.getProperty("java.class.path"), "-d", output.toString()), null, units)
				.call();
			assertThat(compiled).as("Generated source diagnostics: %s", diagnostics.getDiagnostics()).isTrue();
		}
	}

	private Path writeSource(String relativePath, String source) throws IOException {
		Path path = this.temporaryDirectory.resolve(relativePath);
		Files.createDirectories(path.getParent());
		return Files.writeString(path, source);
	}

	private String containerStub() {
		return """
				package types.api;
				public final class Container {
				  public static final class Nested {
				    public static Builder newBuilder() { return new Builder(); }
				    public Builder toBuilder() { return new Builder(); }
				    public static final class Builder {
				      public Builder setQueryText(String value) { return this; }
				      public Nested build() { return new Nested(); }
				    }
				  }
				}
				""";
	}

	private String replyStub() {
		return """
				package service.api;
				public final class Reply {}
				""";
	}

	private String grpcStub() {
		return """
				package service.api;
				import io.grpc.Channel;
				import types.api.Container;
				public final class TestGrpc {
				  public static TestBlockingStub newBlockingStub(Channel channel) { return new TestBlockingStub(); }
				  public static final class TestBlockingStub {
				    public Reply fetch(Container.Nested request) { return new Reply(); }
				  }
				}
				""";
	}

	private HttpRule httpRule(String verb) {
		HttpRule.Builder rule = HttpRule.newBuilder();
		return switch (verb) {
			case "GET" -> rule.setGet("/v1/items/{id}").build();
			case "POST" -> rule.setPost("/v1/items/{id}").setBody("*").build();
			case "PUT" -> rule.setPut("/v1/items/{id}").setBody("*").build();
			case "DELETE" -> rule.setDelete("/v1/items/{id}").build();
			default -> throw new IllegalArgumentException("Unsupported test verb: " + verb);
		};
	}

	private HttpRule unsupportedHttpRule(String feature) {
		return switch (feature) {
			case "named body" -> HttpRule.newBuilder().setPost("/v1/items/{id}").setBody("item").build();
			case "response body" -> HttpRule.newBuilder().setGet("/v1/items/{id}").setResponseBody("result").build();
			case "additional binding" -> HttpRule.newBuilder()
				.setGet("/v1/items/{id}")
				.addAdditionalBindings(HttpRule.newBuilder().setGet("/v1/other/{id}"))
				.build();
			case "unknown path variable" -> HttpRule.newBuilder().setGet("/v1/items/{missing}").build();
			default -> throw new IllegalArgumentException("Unsupported test feature: " + feature);
		};
	}

}
