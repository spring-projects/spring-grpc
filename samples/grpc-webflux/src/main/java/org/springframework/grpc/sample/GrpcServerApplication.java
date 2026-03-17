package org.springframework.grpc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.grpc.client.ImportGrpcClients;

@ImportGrpcClients(basePackageClasses = GrpcServerApplication.class)
@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

}
