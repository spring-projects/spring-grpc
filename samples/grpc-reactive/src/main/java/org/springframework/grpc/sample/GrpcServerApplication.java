package org.springframework.grpc.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;

import io.grpc.Status;

@SpringBootApplication
class GrpcServerApplication {

	private static Log log = LogFactory.getLog(GrpcServerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	GrpcExceptionHandler grpcExceptionHandler() {
		return ex -> {
			if (ex instanceof IllegalArgumentException) {
				log.error("Error in grpc exception", ex);
				return Status.INVALID_ARGUMENT.withDescription(ex.getMessage()).asException();
			}
			return Status.INTERNAL.withCause(ex).withDescription(ex.getMessage()).asException();
		};
	}

}
