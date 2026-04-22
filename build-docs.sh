#!/bin/bash

# Generate the README.md, CONTRIBUTING.md and the Antora site
./mvnw -pl spring-grpc-docs package antora docs
