# Spring Boot gRPC Sample

This project is a copy one of the samples from the [gRPC Spring Boot Starter](https://github.com/yidongnan/grpc-spring-boot-starter/blob/master/examples/local-grpc-server/build.gradle). Build and run any way you like to run Spring Boot. E.g:

```
$ ./mvnw spring-boot:run
...
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.4.5)
2022-12-08T05:32:24.934-08:00  INFO 551632 --- [           main] com.example.demo.DemoApplication         : Starting DemoApplication using Java 17.0.5 with PID 551632 (/home/dsyer/dev/scratch/demo/target/classes started by dsyer in /home/dsyer/dev/scratch/demo)
2022-12-08T05:32:24.938-08:00  INFO 551632 --- [           main] com.example.demo.DemoApplication         : No active profile set, falling back to 1 default profile: "default"
2022-12-08T05:32:25.377-08:00  WARN 551632 --- [           main] ocalVariableTableParameterNameDiscoverer : Using deprecated '-debug' fallback for parameter name resolution. Compile the affected code with '-parameters' instead or avoid its introspection: net.devh.boot.grpc.server.autoconfigure.GrpcHealthServiceAutoConfiguration
2022-12-08T05:32:25.416-08:00  WARN 551632 --- [           main] ocalVariableTableParameterNameDiscoverer : Using deprecated '-debug' fallback for parameter name resolution. Compile the affected code with '-parameters' instead or avoid its introspection: net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration
2022-12-08T05:32:25.425-08:00  WARN 551632 --- [           main] ocalVariableTableParameterNameDiscoverer : Using deprecated '-debug' fallback for parameter name resolution. Compile the affected code with '-parameters' instead or avoid its introspection: net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration
2022-12-08T05:32:25.427-08:00  INFO 551632 --- [           main] g.s.a.GrpcServerFactoryAutoConfiguration : Detected grpc-netty: Creating NettyGrpcServerFactory
2022-12-08T05:32:25.712-08:00  INFO 551632 --- [           main] n.d.b.g.s.s.AbstractGrpcServerFactory    : Registered gRPC service: Simple, bean: grpcServerService, class: com.example.demo.GrpcServerService
2022-12-08T05:32:25.712-08:00  INFO 551632 --- [           main] n.d.b.g.s.s.AbstractGrpcServerFactory    : Registered gRPC service: grpc.health.v1.Health, bean: grpcHealthService, class: io.grpc.protobuf.services.HealthServiceImpl
2022-12-08T05:32:25.712-08:00  INFO 551632 --- [           main] n.d.b.g.s.s.AbstractGrpcServerFactory    : Registered gRPC service: grpc.reflection.v1alpha.ServerReflection, bean: protoReflectionService, class: io.grpc.protobuf.services.ProtoReflectionService
2022-12-08T05:32:25.820-08:00  INFO 551632 --- [           main] n.d.b.g.s.s.GrpcServerLifecycle          : gRPC Server started, listening on address: *, port: 9090
2022-12-08T05:32:25.831-08:00  INFO 551632 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 1.264 seconds (process running for 1.623)
```

The server starts by default on port 9090. Test with [gRPCurl](https://github.com/fullstorydev/grpcurl):

```
$ grpcurl -d '{"name":"Hi"}' -plaintext localhost:9090 Simple.SayHello
{
  "message": "Hello ==\u003e Hi"
}
```

## Native Image
[Native Image with Gradle](./HELP-gradle.md)

[Native Image with Maven](./HELP-maven.md)
