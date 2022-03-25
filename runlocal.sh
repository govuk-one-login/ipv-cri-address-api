#!/usr/bin/env bash
set -eu
./gradlew
sam build -t infrastructure/lambda/template.yaml --config-env dev
sam validate -t infrastructure/lambda/template.yaml --config-env dev
sam local invoke PostcodeLookupFunction -e postcode.event
