# di-ipv-cri-address-api
Address Collector Credential Issuer API

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


