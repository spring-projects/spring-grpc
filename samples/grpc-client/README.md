# Spring Boot gRPC Client Sample

This project is a sample of a gRPC Client app with no server. Build and run any way you like to run Spring Boot. E.g:

```
$ ./mvnw spring-boot:run
...
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.0)

...
2025-02-27T09:21:19.515Z  INFO 1211091 --- [grpc-client] [           main] o.s.g.sample.GrpcClientApplication  : Started GrpcClientApplication in 0.909 seconds (process running for 1.172)
message: "Hello ==> Alien"
```

The server has to be running on port 9090. You can use the `grpc-server` sample for that.

You can also build and run the application as a native image using GraalVM, in the normal way for a Spring Boot application. E.g:

```
$ ./mvnw -Pnative native:compile
$ ./target/grpc-client-sample
...
