#!/usr/bin/env bash
set -eu
sam validate -t infrastructure/lambda/template.yaml
sam build -t infrastructure/lambda/template.yaml --config-env dev
sam deploy --config-env dev --config-file infrastructure/lambda/samconfig.toml --no-fail-on-empty-changeset
