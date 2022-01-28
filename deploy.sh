#!/usr/bin/env bash
./gradlew clean buildZip
sam build -t deploy/template.yaml --config-env dev
sam validate -t deploy/template.yaml --config-env dev
sam deploy -t deploy/template.yaml --config-env dev --no-fail-on-empty-changeset