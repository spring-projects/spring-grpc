package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(GrpcTranscodingIntegrationTests.InterceptorConfiguration.class)
class GrpcTranscodingIntegrationTests {

	@LocalServerPort
	private int port;

	private RestClient restClient;

	@Autowired
	private AtomicInteger interceptedCalls;

	@Autowired
	private AtomicReference<HelloRequest> interceptedRequest;

	@BeforeEach
	void setUp() {
		this.interceptedCalls.set(0);
		this.interceptedRequest.set(null);
		this.restClient = RestClient.builder().baseUrl("http://localhost:" + this.port).build();
	}

	@Test
	void pathParameterIsBound() {
		String body = getHello();

		assertThat(body).contains("Hello ==> World");
		assertThat(capturedRequest()).isEqualTo(request().build());
	}

	@Test
	void queryParameterIsBound() {
		getHello("note", "hello");

		assertThat(capturedRequest()).isEqualTo(request().setNote("hello").build());
	}

	@Test
	void pathParameterOverridesRequestBody() {
		this.restClient.post().uri("/v1/hello/Path").contentType(MediaType.APPLICATION_JSON).body("""
				{
					"name": "Body",
					"note": "from-body"
				}
				""").retrieve().toBodilessEntity();

		assertThat(capturedRequest()).isEqualTo(request().setName("Path").setNote("from-body").build());
	}

	@Test
	void globalClientInterceptorAppliesToTranscodingChannel() {
		getHello();

		assertThat(this.interceptedCalls).hasValue(1);
	}

	@Test
	void duplicateQueryParameterReturns400() {
		RestClientResponseException exception = getHelloError("note", List.of("one", "two"));

		assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(this.interceptedRequest).hasValue(null);
	}

	private String getHello() {
		return this.restClient.get().uri("/v1/hello/World").retrieve().body(String.class);
	}

	private String getHello(String name, String value) {
		return this.restClient.get()
			.uri((builder) -> builder.path("/v1/hello/World").queryParam(name, value).build())
			.retrieve()
			.body(String.class);
	}

	private RestClientResponseException getHelloError(String name, List<String> values) {
		return catchThrowableOfType(() -> this.restClient.get()
			.uri((builder) -> builder.path("/v1/hello/World").queryParam(name, values.toArray()).build())
			.retrieve()
			.toBodilessEntity(), RestClientResponseException.class);
	}

	private HelloRequest capturedRequest() {
		assertThat(this.interceptedRequest.get()).as("request captured by the gRPC server").isNotNull();
		return this.interceptedRequest.get();
	}

	private static HelloRequest.Builder request() {
		return HelloRequest.newBuilder().setName("World");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class InterceptorConfiguration {

		@Bean
		AtomicInteger interceptedCalls() {
			return new AtomicInteger();
		}

		@Bean
		AtomicReference<HelloRequest> interceptedRequest() {
			return new AtomicReference<>();
		}

		@Bean
		@GlobalClientInterceptor
		ClientInterceptor countingClientInterceptor(AtomicInteger interceptedCalls) {
			return new ClientInterceptor() {
				@Override
				public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
						CallOptions callOptions, Channel next) {
					interceptedCalls.incrementAndGet();
					return next.newCall(method, callOptions);
				}
			};
		}

		@Bean
		@GlobalServerInterceptor
		ServerInterceptor capturingServerInterceptor(AtomicReference<HelloRequest> interceptedRequest) {
			return new ServerInterceptor() {
				@Override
				public <RequestT, ResponseT> ServerCall.Listener<RequestT> interceptCall(
						ServerCall<RequestT, ResponseT> call, Metadata headers,
						ServerCallHandler<RequestT, ResponseT> next) {
					ServerCall.Listener<RequestT> delegate = next.startCall(call, headers);
					return new ForwardingServerCallListener.SimpleForwardingServerCallListener<>(delegate) {
						@Override
						public void onMessage(RequestT message) {
							if (message instanceof HelloRequest request) {
								interceptedRequest.set(request);
							}
							super.onMessage(message);
						}
					};
				}
			};
		}

	}

}
