#!/bin/bash

# Be sure the ConfigurationPropertiesAsciidocGenerator is compiled
./mvnw -pl spring-grpc-docs package

# Generate the config props and antora site
./mvnw -pl spring-grpc-docs process-resources antora -P docs
