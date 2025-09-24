# Spring gRPC Docs

## README

The top level README and CONTRIBUTING guidelines documentation are generated from sources in this module on `mvn package` using [`asciidoctor-reducer`](https://github.com/asciidoctor/asciidoctor-reducer) and [`downdoc`](https://github.com/opendevise/downdoc).

## Antora Site

To build the Antora site locally run the following command from the project root directory:
```
./mvnw -pl spring-grpc-docs process-resources antora -P docs
```
You can then view the output by opening `spring-grpc-docs/target/antora/site/index.html`. 
