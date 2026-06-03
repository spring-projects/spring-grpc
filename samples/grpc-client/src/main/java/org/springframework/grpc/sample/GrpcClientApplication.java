package org.springframework.grpc.sample;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;

@SpringBootApplication
@ImportGrpcClients
public class GrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcClientApplication.class, args);
	}

	@Bean
	public CommandLineRunner runner(SimpleGrpc.SimpleBlockingStub stub) {
		return args -> {
			System.out.println(stub.sayHello(HelloRequest.newBuilder().setName("Alien").build()));
		};
	}

}