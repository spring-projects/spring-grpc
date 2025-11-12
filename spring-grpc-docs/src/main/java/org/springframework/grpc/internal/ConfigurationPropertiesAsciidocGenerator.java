/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.grpc.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Generate Asciidoc for configuration properties.
 * <p>
 * Copied from 'spring-cloud-build' to avoid direct dependency on Spring Cloud.
 *
 * @author Marcin Grzejszczak
 * @author Chris Bono
 */
public class ConfigurationPropertiesAsciidocGenerator {

	public static void main(String... args) {
		String outputFile = args[0];
		String inclusionPattern = args.length > 1 ? args[1] : ".*";
		File parent = new File(outputFile).getParentFile();
		if (!parent.exists()) {
			System.out.println("No parent directory [" + parent.toString()
					+ "] found. Will not generate the configuration properties file");
			return;
		}
		new Generator().generate(outputFile, inclusionPattern);
	}

	static class Generator {

		void generate(String outputFile, String inclusionPattern) {
			try {
				System.out.println("Parsing all configuration metadata");
				Resource[] resources = getResources();
				System.out.println("Found [" + resources.length + "] configuration metadata jsons");
				TreeSet<String> names = new TreeSet<>();
				Map<String, ConfigValue> descriptions = new HashMap<>();
				final AtomicInteger count = new AtomicInteger();
				final AtomicInteger matchingPropertyCount = new AtomicInteger();
				final AtomicInteger propertyCount = new AtomicInteger();
				Pattern pattern = Pattern.compile(inclusionPattern);
				for (Resource resource : resources) {
					if (resourceNameContainsPattern(resource)) {
						count.incrementAndGet();
						byte[] bytes = StreamUtils.copyToByteArray(resource.getInputStream());
						Map<String, Object> response = new ObjectMapper().readValue(bytes, HashMap.class);
						List<Map<String, Object>> properties = (List<Map<String, Object>>) response.get("properties");
						properties.forEach(val -> {
							propertyCount.incrementAndGet();
							String name = String.valueOf(val.get("name"));
							if (!pattern.matcher(name).matches()) {
								return;
							}
							Object description = val.get("description");
							Object defaultValue = val.get("defaultValue");
							matchingPropertyCount.incrementAndGet();
							names.add(name);
							descriptions.put(name, new ConfigValue(name, description, defaultValue));
						});
					}
				}
				System.out.println(
						"Found [" + count + "] Spring projects configuration metadata jsons. [" + matchingPropertyCount
								+ "/" + propertyCount + "] were matching the pattern [" + inclusionPattern + "]");
				System.out.println("Successfully built the description table");
				if (names.isEmpty()) {
					System.out.println("Will not update the table, since no configuration properties were found!");
					return;
				}
				Files.write(new File(outputFile).toPath(), ("|===\n" + "|Name | Default | Description\n\n"
						+ names.stream().map(it -> descriptions.get(it).toString()).collect(Collectors.joining("\n"))
						+ "\n\n" + "|===")
					.getBytes());
				System.out.println("Successfully stored the output file");
			}
			catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}

		protected boolean resourceNameContainsPattern(Resource resource) {
			try {
				return resource.getURL().toString().contains("spring");
			}
			catch (Exception e) {
				System.out.println("Exception [" + e + "] for resource [" + resource
						+ "] occurred while trying to retrieve its URL");
				return false;
			}
		}

		protected Resource[] getResources() throws IOException {
			return new PathMatchingResourcePatternResolver()
				.getResources("classpath*:/META-INF/*spring-configuration-metadata.json");
		}

	}

	static class ConfigValue {

		public String name;

		public String description;

		public String defaultValue;

		ConfigValue() {
		}

		ConfigValue(String name, Object description, Object defaultValue) {
			this.name = name;
			this.description = escapedValue(description);
			this.defaultValue = escapedValue(defaultValue);
		}

		private String escapedValue(Object value) {
			return value != null ? value.toString().replaceAll("\\|", "\\\\|") : "";
		}

		public String toString() {
			return "|" + name + " | " + (StringUtils.hasText(defaultValue) ? ("`+++" + defaultValue + "+++`") : "")
					+ " | " + description;
		}

	}

}
