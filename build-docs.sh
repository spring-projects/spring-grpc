#!/bin/bash

# Generate the antora site
./mvnw -pl spring-grpc-docs process-resources antora
