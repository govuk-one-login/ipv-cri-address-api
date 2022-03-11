#!/usr/bin/env bash
set -eu
./gradlew
sam build -t deploy/template.yaml --config-env dev
sam validate -t deploy/template.yaml --config-env dev
sam local invoke PostcodeLookupFunction -e postcode.event
