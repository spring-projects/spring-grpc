## Migration Guide: Spring gRPC 1.0.x to 1.1.1-SNAPSHOT

This is an autommatically generated migration giode specificially for the samples in this project. There is a generic hand written guide that might also help you migrate in a [Wiki page](https://github.com/spring-projects/spring-grpc/wiki/Spring-gRPC-1.1-Migration-Guide). The headline change is that Spring gRPC 1.1.1-SNAPSHOT autoconfiguration ships as part of Spring Boot 4.1.0. The starter artifacts have moved from the `org.springframework.grpc` group into Spring Boot itself.

### 1. Upgrade Spring Boot

Update your parent/BOM version:

**Maven:**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>4.1.0</version>
</parent>
```

**Gradle:**
```groovy
id 'org.springframework.boot' version '4.1.0'
```

### 2. Remove the Spring gRPC BOM

The spring-grpc-dependencies BOM is no longer needed. Spring Boot 4.1.0 manages all gRPC versions directly. Remove any `dependencyManagement` import of it, and remove the explicit `protobuf-java.version` and `grpc.version` properties you previously had to declare yourself.

**Maven — remove:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.grpc</groupId>
            <artifactId>spring-grpc-dependencies</artifactId>
            <version>1.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**Gradle — remove:**
```groovy
dependencyManagement {
    imports {
        mavenBom "org.springframework.grpc:spring-grpc-dependencies:${springGrpcVersion}"
    }
}
```

Affected samples: all samples.

### 3. Replace Spring gRPC starter coordinates

All starters have moved from `org.springframework.grpc` into `org.springframework.boot`.

| 1.0.x artifact | 1.1.1-SNAPSHOT artifact |
|---|---|
| `org.springframework.grpc:spring-grpc-spring-boot-starter` | `org.springframework.boot:spring-boot-starter-grpc-server` |
| `org.springframework.grpc:spring-grpc-client-spring-boot-starter` | `org.springframework.boot:spring-boot-starter-grpc-client` |
| `org.springframework.grpc:spring-grpc-test` | `org.springframework.boot:spring-boot-starter-grpc-client-test` |

Affected samples: all samples (see e.g. grpc-server/pom.xml, grpc-client/build.gradle).

### 4. Replace the Tomcat/Servlet web starter

The dedicated spring-grpc-server-web-spring-boot-starter is gone. Use the standard gRPC server starter together with the servlet adapter:

**Before:**
```xml
<dependency>
    <groupId>org.springframework.grpc</groupId>
    <artifactId>spring-grpc-server-web-spring-boot-starter</artifactId>
</dependency>
```

**After:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-grpc-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-servlet-jakarta</artifactId>
</dependency>
```

Also add to `application.properties`:
```properties
server.http2.enabled=true
```

Affected samples: grpc-tomcat, grpc-tomcat-secure.

### 5. Update the Gradle Protobuf plugin configuration

Spring Boot now manages `protoc` and the gRPC codegen plugin versions. In Gradle, remove the `protoc` artifact declaration and (for plain Java) the entire `protobuf { ... }` block. You only need to keep the block if you require additional code generators (e.g. Kotlin, Reactor gRPC).

**Before:**
```groovy
protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${dependencyManagement.importedProperties['protobuf-java.version']}"
    }
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${dependencyManagement.importedProperties['grpc.version']}"
        }
    }
    generateProtoTasks {
        all()*.plugins {
            grpc { option '@generated=omit' }
        }
    }
}
```

**After (plain Java — just delete the block).**

**After (Kotlin — keep plugin declarations but drop `protoc`):**
```groovy
protobuf {
    plugins {
        grpc {
            artifact = "io.grpc:protoc-gen-grpc-java:${dependencyManagement.importedProperties['grpc.version']}"
        }
        grpckt {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${dependencyManagement.importedProperties['grpc-kotlin.version']}:jdk8@jar"
        }
    }
    generateProtoTasks { ... }
}
```

Affected samples: grpc-server/build.gradle, grpc-server-kotlin/build.gradle.

Also update the plugin version: `com.google.protobuf` version `0.9.4` → `0.9.6`.

### 6. Update the Maven Protobuf plugin configuration

The `protobuf-maven-plugin` version and its entire `<configuration>` block (containing `<protocVersion>`, `<binaryMavenPlugins>`, and `<executions>`) are now managed by Spring Boot. Remove them if you use the `spring-boot-starter-parent`.

**Before:**
```xml
<plugin>
    <groupId>io.github.ascopes</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
    <version>4.0.3</version>
    <configuration>
        <protocVersion>${protobuf-java.version}</protocVersion>
        <binaryMavenPlugins>
            <binaryMavenPlugin>
                <groupId>io.grpc</groupId>
                <artifactId>protoc-gen-grpc-java</artifactId>
                <version>${grpc.version}</version>
                <options>@generated=omit</options>
            </binaryMavenPlugin>
        </binaryMavenPlugins>
    </configuration>
    <executions>
        <execution><goals><goal>generate</goal></goals></execution>
    </executions>
</plugin>
```

**After (plain Java — just the bare plugin declaration):**
```xml
<plugin>
    <groupId>io.github.ascopes</groupId>
    <artifactId>protobuf-maven-plugin</artifactId>
</plugin>
```

For the `grpc-reactive` sample the plugin element names also changed: `<binaryMavenPlugin>` → `<plugin kind="binary-maven">` and `<jvmMavenPlugin>` → `<plugin kind="jvm-maven">`, and the version property is now `${grpc-java.version}` instead of `${grpc.version}`.

Affected samples: grpc-server/pom.xml, grpc-reactive/pom.xml.

### 7. Rename client channel properties

The `spring.grpc.client` namespace has changed. The key pattern moves from `default-channel`/`channels` to `channel`, and `address` becomes target.

| 1.0.x property | 1.1.1-SNAPSHOT property |
|---|---|
| `spring.grpc.client.default-channel.address=...` | `spring.grpc.client.channel.default.target=...` |
| `spring.grpc.client.channels.<name>.address=...` | `spring.grpc.client.channel.<name>.target=...` |
| `spring.grpc.client.default-channel.default-deadline=...` | `spring.grpc.client.channel.default.default.deadline=...` |
| `spring.grpc.client.channels.<name>.health.enabled=...` | `spring.grpc.client.channel.<name>.health.enabled=...` |
| `spring.grpc.client.channels.<name>.negotiation-type=TLS` + `.secure=false` | `spring.grpc.client.channel.<name>.ssl.enabled=true` + `.bypass-certificate-validation=true` |
| `spring.grpc.client.channels.<name>.ssl.bundle=...` | `spring.grpc.client.channel.<name>.ssl.bundle=...` |

Affected samples: grpc-client/application.properties, grpc-server/GrpcServerIntegrationTests.java, grpc-secure/GrpcServerApplicationTests.java.

### 8. Rename server properties

| 1.0.x property | 1.1.1-SNAPSHOT property |
|---|---|
| `spring.grpc.server.host=<ip>` | `spring.grpc.server.address=<ip>` |
| `spring.grpc.server.address=unix:<path>` | `spring.grpc.server.netty.domain-socket-path=<path>` |
| `spring.grpc.server.health.actuator.health-indicator-paths=<svc>` | `spring.grpc.server.health.service.<svc>.include=<svc>` |
| `spring.grpc.server.health.actuator.update-initial-delay=` + `update-rate=` | `spring.grpc.server.health.schedule.delay=` + `.period=` |

Affected sample: grpc-server/GrpcServerIntegrationTests.java, grpc-server/GrpcServerHealthIntegrationTests.java.

Also, the `local.grpc.port` placeholder used in test properties is now `local.grpc.server.port`.

### 9. Update test annotations

| 1.0.x | 1.1.1-SNAPSHOT |
|---|---|
| `@AutoConfigureInProcessTransport` | `@AutoConfigureTestGrpcTransport` |
| `@LocalGrpcPort` | `@LocalGrpcServerPort` |

Affected samples: grpc-server/GrpcServerIntegrationTests.java, grpc-server-netty-shaded/DemoApplicationTests.java, grpc-reactive/GrpcServerApplicationTests.java.

### 10. Add `@ImportGrpcClients` wherever stubs are injected

gRPC client stubs are no longer auto-registered. Any class (application, configuration, or test) that injects stubs must declare `@ImportGrpcClients` to trigger stub registration. For tests a `@TestConfiguration` inner class annotated with it is the typical pattern.

```java
@SpringBootTest
@ImportGrpcClients   // required to register stubs as beans
class MyGrpcTests { ... }
```

Or in a `@TestConfiguration`:

```java
@TestConfiguration
@ImportGrpcClients(basePackageClasses = GrpcServerApplication.class)
static class ExtraConfiguration { }
```

Affected samples: grpc-client/GrpcClientApplicationTests.java, grpc-server/GrpcServerSideTests.java, grpc-server-kotlin/GrpcServerApplication.kt, grpc-tomcat/GrpcServerApplicationTests.java.

