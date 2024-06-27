# Address Collector Credential Issuer API

## Hooks

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting
   
## Deploy to dev account

Ensure you have the `sam-cli` and `gds-cli` installed, and that you can assume an admin role on the ` di-ipv-cri-address-dev` AWS account.
Alternatively you can [create a sso profile](https://govukverify.atlassian.net/wiki/spaces/LO/pages/3725591061/Getting+set+up+with+AWS+SSO+in+terminal+CLI+-+quickstart)

Any time you wish to deploy, run:

`gds aws  di-ipv-cri-address-dev -- ./deploy.sh`

or with an AWS SSO profile

`AWS_PROFILE=profile-name-you-created aws  di-ipv-cri-address-dev -- ./deploy.sh`

The Stack Name, CommonStackName and SecretPrefix are optional, but can be overridden by supplying

additional arguments to `deploy.sh` i.e

gds aws  di-ipv-cri-address-dev -- ./deploy.sh STACKNAME YOUR-COMMON-STACKNAME YOUR-SECRET-PREFIX

## Deploy to AWS lambda

Automated GitHub actions deployments to di-ipv-cri-build have been enabled for this repository.
Manual GitHub actions deployments to di-ipv-cri-address-dev can be triggered from the GitHub actions menu.

The automated deployments are triggered on a push to main after PR approval.

GitHub secrets are required for deployment.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| ARTIFACT_SOURCE_BUCKET_NAME | Upload artifact bucket |
| GH_ACTIONS_ROLE_ARN | Assumed role IAM ARN |
| SIGNING_PROFILE_NAME | Signing profile name |

For Dev the following equivalent GitHub secrets:

| Secret                          | Description |
|---------------------------------| ----------- |
| DEV_ARTIFACT_SOURCE_BUCKET_NAME | Upload artifact bucket |
| DEV_GH_ACTIONS_ROLE_ARN         | Assumed role IAM ARN |
| DEV_SIGNING_PROFILE_NAME        | Signing profile name |

## Publishing KMS Public keys

The Address API uses an AWS KMS EC private key to sign verifiable credentials,
and an AWS KMS RSA private key to decrypt the Authorization JAR.

The public keys need to be published so that clients:
* can verify the signature of the verifiable credential,
* encrypt the Authorization JAR before sending to this CRI.

The environment variable `IPV_CORE_STUB_CRI_ID` with value `address-cri-dev` allows the command below to use keys in `ipv-config` pointing to keys in `di-ipv-cri-address-dev` for the deployed stack in that account.

## Run all tests
Make sure you have deployed a stack on AWS and provide that `STACK_NAME` below with corresponding `API_GATEWAY_ID_PRIVATE` and `API_GATEWAY_ID_PUBLIC` endpoints


Below runs by overriding the stub client to `https://cri.core.build.stubs.account.gov.uk` in AWS with stub a client_id ipv-core-stub-aws-stub using DEFAULT_CLIENT_ID env variable

```
ENVIRONMENT=dev STACK_NAME=xxxx IPV_CORE_STUB_CRI_ID=address-cri-dev  API_GATEWAY_ID_PRIVATE=xxxx API_GATEWAY_ID_PUBLIC=xxxx IPV_CORE_STUB_BASIC_AUTH_USER=xxxx IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL="https://cri.core.build.stubs.account.gov.uk" DEFAULT_CLIENT_ID=ipv-core-stub-aws-build APIGW_API_KEY=xxxx gradle integration-tests:cucumber
```

## Run a particular test
````
STACK_NAME=xxxx IPV_CORE_STUB_CRI_ID=address-cri-dev ENVIRONMENT=dev API_GATEWAY_ID_PRIVATE=xxxx  API_GATEWAY_ID_PUBLIC=xxxx IPV_CORE_STUB_BASIC_AUTH_USER=xxxx IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL="https://di-ipv-core-stub.london.cloudapps.digital" APIGW_API_KEY=xxxx gradle cucumber -P tags=@tag-name
````
