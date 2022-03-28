#!/usr/bin/env bash
set -e
./gradlew
if [ -z "$1" ]
then
	echo "Please pass the name of a '.event' file to pass to the event handler."

else
  sam build -t infrastructure/lambda/template.yaml --config-env dev
  sam validate -t infrastructure/lambda/template.yaml --config-env dev
  case $1 in
    postcode)
      sam local invoke PostcodeLookupFunction -e "$1.event"
      ;;
    address)
      sam local invoke AddressFunction -e "$1.event"
      ;;
    session)
      sam local invoke SessionFunction -e "$1.event"
      ;;
  esac
fi
