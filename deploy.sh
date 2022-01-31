#!/usr/bin/env bash

./gradlew clean buildZip
sam build -t deploy/template.yaml --config-env dev
sam validate -t deploy/template.yaml --config-env dev
aws s3 cp deploy/api.yaml s3://di-ipv-address-cri-api/dev/api.yaml
sam deploy -t deploy/template.yaml --config-env dev --config-file samconfig.toml --no-fail-on-empty-changeset
