#!/usr/bin/env bash
set -eu
./gradlew
sam build -t deploy/template.yaml --config-env dev
sam validate -t deploy/template.yaml --config-env dev
sam deploy -t deploy/template.yaml --config-env dev --config-file samconfig.toml --no-fail-on-empty-changeset
