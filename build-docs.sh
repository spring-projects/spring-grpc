#!/bin/bash

# Generate the README.md, CONTRIBUTING.md and the Antora site
./mvnw package -DskipTests
./mvnw -pl spring-grpc-docs package antora
