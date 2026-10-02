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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PathTemplate}.
 *
 * @author Aleksander Brzozowski
 */
class PathTemplateTests {

	@Test
	void singleVariable() {
		PathTemplate template = PathTemplate.parse("/v1/hello/{name}");
		assertThat(template.getVariables()).containsExactly("name");
		assertThat(template.getPattern()).isEqualTo("/v1/hello/{name}");
	}

	@Test
	void multipleVariables() {
		PathTemplate template = PathTemplate.parse("/v1/users/{user_id}/posts/{post_id}");
		assertThat(template.getVariables()).containsExactly("user_id", "post_id");
	}

	@Test
	void noVariables() {
		PathTemplate template = PathTemplate.parse("/v1/noVars");
		assertThat(template.getVariables()).isEmpty();
	}

	@Test
	void interleavedVariables() {
		PathTemplate template = PathTemplate.parse("/{a}/b/{c}");
		assertThat(template.getVariables()).containsExactly("a", "c");
	}

	@Test
	void underscorePrefixedVariable() {
		PathTemplate template = PathTemplate.parse("/{__id}");
		assertThat(template.getVariables()).containsExactly("__id");
	}

	@Test
	void patternIsPreservedVerbatim() {
		String raw = "/v1/items/{item_id}/sub/{sub_id}";
		PathTemplate template = PathTemplate.parse(raw);
		assertThat(template.getPattern()).isEqualTo(raw);
	}

	@Test
	void rejectsUnsupportedAndMalformedTemplates() {
		assertThatThrownBy(() -> PathTemplate.parse("v1/items")).isInstanceOf(GenerationException.class);
		assertThatThrownBy(() -> PathTemplate.parse("/v1/{name=items/*}")).isInstanceOf(GenerationException.class);
		assertThatThrownBy(() -> PathTemplate.parse("/v1/{parent.name}")).isInstanceOf(GenerationException.class);
		assertThatThrownBy(() -> PathTemplate.parse("/v1/{name}/{name}")).isInstanceOf(GenerationException.class)
			.hasMessageContaining("duplicate path variable");
	}

}
