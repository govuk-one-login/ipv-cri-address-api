# di-ipv-cri-address-api
Address Collector Credential Issuer API

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew clean build`

## Deploy to dev account

Before your **first** deploy, build a sam config toml file.
> The stack name *must* be unique to you.
> **Ensure you change the environment name**, when asked, to `dev` instead of `default`.
> All other defaults can be accepted by leaving them blank

The command to run is: 

`gds aws <account> -- sam deploy -t infrastructure/lambda/template.yaml --guided`

You will be asked for the Ordnance Survey API Key at this point.
In production, this key is stored in the AWS Secrets Manager.

Any time you wish to deploy, run:

`gds aws <account> -- ./deploy.sh`

If you wish to test your lambda functions locally, you can update the "postcode.event" file and then run:

`gds aws <account> -- ./runlocal.sh`


## Deploy to AWS lambda

Automated GitHub actions deployments to di-ipv-cri-build have been enabled for this repository.
Manual GitHub actions deployments to di-ipv-cri-address-dev can be triggered from the GitHub actions menu.

The automated deployments are triggered on a push to main after PR approval.

GitHub secrets are required which must be configured in an environment for security reasons.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| ARTIFACT_SOURCE_BUCKET_NAME | Upload artifact bucket |
| GH_ACTIONS_ROLE_ARN | Assumed role IAM ARN |
| SIGNING_PROFILE_NAME | Signing profile name |

Additional GitHub secrets for the deployment of the parameters

| Secret | Description |
| ------ | ----------- |
| PARAM_ARTIFACT_SOURCE_BUCKET_NAME | Upload artifact bucket |
| PARAM_GH_ACTIONS_ROLE_ARN | Assumed role IAM ARN |
| PARAM_SIGNING_PROFILE_NAME | Signing profile name |
