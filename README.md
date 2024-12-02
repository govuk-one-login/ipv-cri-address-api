# Address Collector Credential Issuer API

## Hooks

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.test
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

## Canaries
When deploying using sam deploy, canary deployment strategy will be used which is set in LambdaDeploymentPreference in template.yaml file. 

When deploying using the pipeline, canary deployment strategy set in the pipeline will be used and override the default set in template.yaml. 

Canary deployments will cause a rollback if any canary alarms associated with a lambda are triggered. 

To skip canaries such as when releasing urgent changes to production, set the last commit message to contain either of these phrases: [skip canary], [canary skip], or [no canary] as specified in the [Canary Escape Hatch guide](https://govukverify.atlassian.net/wiki/spaces/PLAT/pages/3836051600/Rollback+Recovery+Guidance#Escape-Hatch%3A-how-to-skip-canary-deployments-when-needed). 
`git commit -m "some message [skip canary]"`

Note: To update LambdaDeploymentPreference, update the LambdaCanaryDeployment pipeline parameter in the [identity-common-infra repository](https://github.com/govuk-one-login/identity-common-infra/tree/main/terraform/orange/address). To update the LambdaDeploymentPreference for a stack in dev using sam deploy, parameter override needs to be set in the [deploy script](./deploy.sh). 
`--parameter-overrides LambdaDeploymentPreference=<define-strategy> \`

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

## Integration tests

Make sure you have deployed a stack to AWS and provide its name in the `STACK_NAME` variable below with the corresponding values for `API_GATEWAY_ID_PRIVATE` and `API_GATEWAY_ID_PUBLIC`.

To initiate journeys for the tests we use the IPV Core Stub, which runs in AWS and is accessible at `https://cri.core.build.stubs.account.gov.uk`.

The command below overrides the client ID used by the Core Stub to `ipv-core-stub-aws-prod` by setting the `DEFAULT_CLIENT_ID` environment variable.

Optionally set a value for `TEST_RESOURCES_STACK_NAME` if you have deployed a local test resources stack and want to override the default stack named `test-resources`.

```
ENVIRONMENT=localdev STACK_NAME=<your-stack> API_GATEWAY_ID_PRIVATE=<from-your-stack> API_GATEWAY_ID_PUBLIC=<from-your-stack> IPV_CORE_STUB_CRI_ID=address-cri-dev IPV_CORE_STUB_BASIC_AUTH_USER=xxxx IPV_CORE_STUB_BASIC_AUTH_PASSWORD=xxxx IPV_CORE_STUB_URL=https://cri.core.build.stubs.account.gov.uk DEFAULT_CLIENT_ID=ipv-core-stub-aws-build APIGW_API_KEY=xxxx TEST_RESOURCES_STACK_NAME= gradle integration-tests:cucumber
```

To run a particular test append `-P tags=@tag-name` to the command above specifying the tag you want to select.
