/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
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
