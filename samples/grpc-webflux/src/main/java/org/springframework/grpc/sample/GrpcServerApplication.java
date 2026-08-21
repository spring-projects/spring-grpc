package org.springframework.grpc.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.protobuf.ProtobufJsonDecoder;
import org.springframework.http.codec.protobuf.ProtobufJsonEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@ImportGrpcClients(basePackageClasses = GrpcServerApplication.class)
@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

}

@Configuration
class GrpcServerConfiguration implements WebFluxConfigurer {

	@Override
	public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
		configurer.customCodecs().register(new ProtobufJsonDecoder());
		configurer.customCodecs().register(new ProtobufHttpMessageWriter(new ProtobufJsonEncoder()));
	}

}
