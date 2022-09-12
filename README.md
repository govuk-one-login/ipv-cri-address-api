# Address Collector Credential Issuer API

## Hooks

**important:** One you've cloned the repo, run `pre-commit install` to install the pre-commit hooks.
If you have not installed `pre-commit` then please do so [here](https://pre-commit.com/).

## Check out submodules (First Time)
> The first time you check out or clone the repository, you will need to run the following commands:

`git submodule update --init --recursive`

## Update submodules (Subsequent times)
> Subsequent times you will need to run the following commands:

`git submodule update --recursive`

## Updating submodules to the latest "main" branch
> You can also update the submodules to the latest "main" branch, but this is not done automatically
> in case there have been changes made to the shared libraries you do not yet want to track

cd into each submodule (folders are `/lib` and `/common-lambdas`) and run the following commands:

`git checkout main && git pull`

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

The command to run is:

`gds aws  di-ipv-cri-dev -- sam deploy -t infrastructure/lambda/template.yaml --guided`

You will be asked for the Ordnance Survey API Key at this point.
In production, this key is stored in the AWS Secrets Manager.

Any time you wish to deploy, run:

`gds aws  di-ipv-cri-dev -- ./deploy.sh`


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
