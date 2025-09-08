# Address Collector Credential Issuer API

## Hooks.   

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
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

Ensure you have the `sam-cli` installed, [create a sso profile](https://govukverify.atlassian.net/wiki/spaces/LO/pages/3725591061/Getting+set+up+with+AWS+SSO+in+terminal+CLI+-+quickstart) for the role `AdministratorAccessPermission` on the ` di-ipv-cri-address-dev` AWS account which can be found by searching the [AWS start page](https://uk-digital-identity.awsapps.com/start#/) .


To deploy a stack, run:

`AWS_PROFILE=profile-name-you-created aws  di-ipv-cri-address-dev -- ./deploy.sh`

The `Stack Name`, `CommonStackName` and `SecretPrefix` are optional, but can be overridden by supplying

additional arguments to `deploy.sh` i.e

AWS_PROFILE=profile-name-you-created ./deploy.sh STACKNAME YOUR-COMMON-STACKNAME YOUR-SECRET-PREFIX

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


## Integration tests

To run these tests the following environment variables are needed:

- STACK_NAME
- AWS_REGION
- AWS_ACCESS_KEY_ID
- AWS_SECRET_ACCESS_KEY
- AWS_SESSION_TOKEN

Temporary credentials can be found by going to the [AWS start page](https://uk-digital-identity.awsapps.com/start#/), selecting the account and clicking
"Command line or programmatic access"


Make sure you have deployed a stack to AWS and provide its name in the `STACK_NAME` variable below with the corresponding values for `API_GATEWAY_ID_PRIVATE` and `API_GATEWAY_ID_PUBLIC`.

To initiate journeys for the tests we use the Headless Core Stub, which runs in AWS and at the following endpoint `https://test-resources.review-a.dev.account.gov.uk`.


Optionally set a value for `TEST_RESOURCES_STACK_NAME` if you have deployed a local test resources stack and want to override the default stack named `test-resources`.

```
ENVIRONMENT=localdev AWS_REGION=eu-west-2 STACK_NAME=<your-stack> API_GATEWAY_ID_PRIVATE=<from-your-stack> API_GATEWAY_ID_PUBLIC=<from-your-stack> APIGW_API_KEY=xxxx TEST_RESOURCES_STACK_NAME= gradle integration-tests:cucumber
```

To run a particular test append `-P tags=@tag-name` to the command above specifying the tag you want to select.
