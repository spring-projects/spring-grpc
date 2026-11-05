/*
 * Copyright and licensing information for this source code can be found in the NOTICE and
 * LICENSE files in the root directory of this repository.
 */

package org.springframework.grpc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusException;

@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	public GrpcExceptionHandler globalInterceptor() {
		return exception -> {
			if (exception instanceof IllegalArgumentException) {
				Metadata metadata = new Metadata();
				metadata.put(Metadata.Key.of("error-code", Metadata.ASCII_STRING_MARSHALLER), "INVALID_ARGUMENT");
				StatusException result = Status.INVALID_ARGUMENT.withDescription(exception.getMessage())
					.asException(metadata);
				return result;
			}
			return null;
		};
	}

}
