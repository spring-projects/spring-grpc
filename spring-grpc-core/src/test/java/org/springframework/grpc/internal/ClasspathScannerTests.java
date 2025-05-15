/*
 * Copyright 2024-2024 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;

public class ClasspathScannerTests {

	@Test
	void testScan() {
		ClasspathScanner scanner = new ClasspathScanner();
		Set<Class<?>> classes = scanner.scan("org.springframework.grpc.internal", ClasspathScannerTests.class);
		assertThat(classes).isNotEmpty();
		assertThat(classes).contains(ClasspathScannerTests.class);
	}

	@Test
	void testFilter() {
		ClasspathScanner scanner = new ClasspathScanner();
		Set<Class<?>> classes = scanner.scan("org.springframework.grpc.internal", new AssignableTypeFilter(Foo.class));
		assertThat(classes).isNotEmpty();
		assertThat(classes).contains(Foo.class);
	}

	@Test
	void testNoFilter() {
		ClasspathScanner scanner = new ClasspathScanner();
		Set<Class<?>> classes = scanner.scan("org.springframework.grpc.internal", (TypeFilter) null);
		assertThat(classes).isEmpty();
	}

	@Test
	void testAnnotation() {
		ClasspathScanner scanner = new ClasspathScanner();
		Set<Class<?>> classes = scanner.annotated("org.springframework.grpc.internal", FooMarker.class);
		assertThat(classes).isNotEmpty();
		assertThat(classes).contains(Foo.class);
	}

	@FooMarker
	interface Foo {

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface FooMarker {

	}

}
