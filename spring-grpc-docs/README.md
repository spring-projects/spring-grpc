# Spring gRPC Docs

## README

The top level README and CONTRIBUTING guidelines documentation are generated from sources in this module on `mvn package` using [`asciidoctor-reducer`](https://github.com/asciidoctor/asciidoctor-reducer) and [`downdoc`](https://github.com/opendevise/downdoc).

## Configuration Properties
The Spring gRPC configuration properties are automatically documented as follows:

1. This module contains a Java class (`org.springframework.grpc.internal.ConfigurationPropertiesAsciidocGenerator`) that is compiled when the module is built.
1. This class is then used during the Maven `process-resources` phase in the `docs` profile to generate an asciidoc page containing each of the configuration properties.
1. The asciidoc is then included in the Antora reference documentation.

## Antora Site

To build the Antora site locally run the following command from the project root directory:
```
./mvnw -pl spring-grpc-docs process-resources antora -P docs
```
You can then view the output by opening `spring-grpc-docs/target/antora/site/index.html`. 
