#!/usr/bin/env bash
set -eu
sam validate -t infrastructure/lambda/template.yaml
sam build -t infrastructure/lambda/template.yaml --config-env dev
sam deploy --config-file infrastructure/lambda/samconfig.toml \
   --config-env dev \
   --no-fail-on-empty-changeset \
   --parameter-overrides   CodeSigningEnabled=false \
   AuditEventNamePrefix=/common-cri-parameters/AddressAuditEventNamePrefix \
   CriIdentifier=/common-cri-parameters/AddressCriIdentifier \
   CommonStackName=address-common-cri-api