package org.springframework.grpc.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.util.MimeType;
import org.springframework.web.reactive.function.client.WebClient;

import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = { "spring.grpc.server.port=0" }, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@AutoConfigureWebClient
public class JsonIntegrationTests {

	public static void main(String[] args) {
		new SpringApplicationBuilder(GrpcServerApplication.class).run();
	}

	private WebClient rest;

	@BeforeEach
	void setUp(@Autowired WebClient.Builder builder, @LocalServerPort int port) {
		this.rest = builder.codecs(config -> {
			// Extra codecs are needed, but they should not be
			MimeType[] mimeTypes = new MimeType[] { MediaType.APPLICATION_JSON, new MediaType("application", "*+json"),
					MediaType.APPLICATION_NDJSON, MediaType.valueOf("application/*+x-ndjson") };
			JacksonJsonDecoder decoder = new JacksonJsonDecoder(JsonMapper.builder(), mimeTypes);
			config.defaultCodecs().jacksonJsonDecoder(decoder);
		}).baseUrl("http://localhost:" + port).build();
	}

	@Test
	void serverResponds() {
		Bar response = rest.post()
			.uri("Json/SayHello")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(new Foo("Alien"))
			.retrieve()
			.bodyToMono(Bar.class)
			.block();
		assertEquals("Hello ==> Alien", response.getMessage());
	}

	@Test
	void serverStreams() {
		Bar response = rest.post()
			.uri("Json/StreamHello")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(new Foo("Alien"))
			.retrieve()
			.bodyToFlux(Bar.class)
			.blockFirst();
		assertEquals("Hello(0) ==> Alien", response.getMessage());
	}

	static class Foo {

		private String name;

		public Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	static class Bar {

		private String message;

		public Bar() {
		}

		public Bar(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String name) {
			this.message = name;
		}

	}

}