package org.springframework.grpc.sample;

import org.apache.coyote.UpgradeProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(GrpcServerApplication.class, args);
	}

	@Bean
	public TomcatConnectorCustomizer customizer() {
		return (connector) -> {
			for (UpgradeProtocol protocol : connector.findUpgradeProtocols()) {
				if (protocol instanceof Http2Protocol http2Protocol) {
					http2Protocol.setOverheadWindowUpdateThreshold(0);
				}
			}
		};
	}

}
