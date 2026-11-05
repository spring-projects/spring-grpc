/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.server.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.scope.EnableGrpcScope;

class GrpcRequestScopeTests {

	@Test
	void testGrpcRequestScopeRegistration() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
			assertThat(context.containsBean("testBean")).isTrue();
		}
	}

	@Configuration
	@EnableGrpcScope
	static class TestConfig {

		@Bean
		@GrpcRequestScope
		public TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

	}

}
