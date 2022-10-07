# Address Collector Credential Issuer API

## Hooks

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew`

This will run "build", "test", "buildZip", and "spotLess" reformatting

## Test Locally
Ensure you have built the project using the command above

At present, only two lambdas can be tested locally, however this can easily extended.

To test the `Address` lambda, run the following command:

`gds aws di-ipv-cri-dev -- ./runlocal.sh address`

This will pass data from the file "address.event" to the lambda.

To test the `PostcodeLookup` lambda, run the following command:

`gds aws di-ipv-cri-dev -- ./runlocal.sh postcode`



## Deploy to dev account

Before your **first** deploy, build a sam config toml file.
> The stack name *must* be unique to you.
> **Ensure you change the environment name**, when asked, to `dev` instead of `default`.
> All other defaults can be accepted by leaving them blank

Any time you wish to deploy, run:

`gds aws  di-ipv-cri-dev -- ./deploy.sh STACKNAME`

Make sure you replace `STACKNAME` with your stack name that you want to deploy to.

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

The `JWKSetHandler` lambda function publishes these public keys as a JWKSet to `https://${AddressApi}.execute-api.${AWS::Region}.amazonaws.com/${Environment}/.well-known/jwks.json`.

KMS keys must be marked for publishing by setting the following AWS tags on the KMS resources in the SAM template:
````
Tags:
- Key: "jwkset"
  Value: "true"
- Key: "awsStackName"
  Value: !Sub "${AWS::StackName}"
````

The `JWKSetHandler` lambda function must be supplied the stack name it is published to the `AWS_STACK_NAME` environment variable:

````
Environment:
      Variables:
        AWS_STACK_NAME: !Sub ${AWS::StackName}
````
