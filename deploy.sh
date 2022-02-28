#!/usr/bin/env bash

./gradlew clean build
sam build -t deploy/template.yaml --config-env dev
sam validate -t deploy/template.yaml --config-env dev
sam deploy -t deploy/template.yaml --config-env dev --config-file samconfig.toml --no-fail-on-empty-changeset
