# Spring Boot gRPC Sample

This project is a copy one of the samples from the [gRPC Spring Boot Starter](https://github.com/yidongnan/grpc-spring-boot-starter/blob/master/examples/local-grpc-server/build.gradle). Build and run any way you like to run Spring Boot. E.g:

```
$ ./mvnw spring-boot:test-run
...
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.5.0)

...
2022-12-08T05:32:25.427-08:00  INFO 551632 --- [           main] g.s.a.GrpcServerFactoryAutoConfiguration : Detected grpc-netty: Creating NettyGrpcServerFactory
2025-01-28T07:10:35.363Z  INFO 1218185 --- [           main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 44207 (http) with context path '/'
2025-01-28T07:10:35.394Z  INFO 1218185 --- [           main] o.s.e.b.s.e.m.SpringBootApplicationMain  : Started SpringBootApplicationMain in 2.802 seconds (process running for 3.291)
2025-01-28T07:10:35.749Z  INFO 1218050 --- [grpc-server] [           main] o.s.grpc.server.NettyGrpcServerFactory   : Registered gRPC service: Simple
2025-01-28T07:10:35.749Z  INFO 1218050 --- [grpc-server] [           main] o.s.grpc.server.NettyGrpcServerFactory   : Registered gRPC service: grpc.reflection.v1.ServerReflection
2025-01-28T07:10:35.749Z  INFO 1218050 --- [grpc-server] [           main] o.s.grpc.server.NettyGrpcServerFactory   : Registered gRPC service: grpc.health.v1.Health
2025-01-28T07:10:35.835Z  INFO 1218050 --- [grpc-server] [           main] o.s.g.s.lifecycle.GrpcServerLifecycle    : gRPC Server started, listening on address: [/[0:0:0:0:0:0:0:0]:9090]
2025-01-28T07:10:35.844Z  INFO 1218050 --- [grpc-server] [           main] o.s.grpc.sample.GrpcServerApplication    : Started GrpcServerApplication in 5.072 seconds (process running for 5.419)
```

The server starts by default on port 9090 and the auth server port is shown on start up (its random for now, until the testjars project has another release). Test with [gRPCurl](https://github.com/fullstorydev/grpcurl).  There is a B/S insecure channel that lets you authenticate by asserting in a header that you are a user:

```
$ TOKEN=`curl -v spring:secret@localhost:43737/oauth2/token -d grant_type=client_credentials | jq -r .access_token`
$ grpcurl -H "Authorization: Bearer $TOKEN" -d '{"name":"Hi"}' -plaintext localhost:9090 Simple.SayHello
{
  "message": "Hello ==\u003e Hi"
}
```
