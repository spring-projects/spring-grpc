package org.springframework.grpc.sample;

import java.time.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.grpc.stub.StreamObserver;
import reactor.core.publisher.Flux;

@Service
@RestController
public class GrpcServerService extends SimpleGrpc.SimpleImplBase {

	private static Log log = LogFactory.getLog(GrpcServerService.class);

	@Override
	public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
		log.info("Hello " + req.getName());
		if (req.getName().startsWith("error")) {
			throw new IllegalArgumentException("Bad name: " + req.getName());
		}
		if (req.getName().startsWith("internal")) {
			throw new RuntimeException();
		}
		HelloReply reply = HelloReply.newBuilder().setMessage("Hello ==> " + req.getName()).build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

	@Override
	public void streamHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
		log.info("Hello " + req.getName());
		Flux.interval(Duration.ofSeconds(1))
			.take(10)
			.map(count -> HelloReply.newBuilder().setMessage("Hello(" + count + ") ==> " + req.getName()).build())
			.subscribe(responseObserver::onNext, responseObserver::onError, responseObserver::onCompleted);
	}

	@PostMapping(path = "Json/SayHello", produces = "application/json")
	public HelloReply sayHello(@RequestBody HelloRequest req) {
		SingleValueObserver<HelloReply> observer = new SingleValueObserver<>();
		sayHello(req, observer);
		return observer.getValue();
	}

	@PostMapping(path = "Json/StreamHello", produces = "application/x-ndjson")
	public Flux<HelloReply> stream(@RequestBody HelloRequest req) {
		MultiValueObserver<HelloReply> observer = new MultiValueObserver<>();
		streamHello(req, observer);
		return observer.getValue();
	}

}
