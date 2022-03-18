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

`gds aws <account> -- sam deploy -t deploy/template.yaml --guided`

You will be asked for the Ordnance Survey API Key at this point.
In production, this key is stored in the AWS Secrets Manager.

Any time you wish to deploy, run:

`gds aws <account> -- ./deploy.sh`

If you wish to test your lambda functions locally, you can update the "postcode.event" file and then run:

`gds aws <account> -- ./runlocal.sh`


## Deploy to AWS lambda

Automated GitHub actions deployments to di-ipv-cri-dev and di-ipv-cri-address-build have been enabled for this repository.

The automated deployments are triggered on a push to main after PR approval.

There are two environments required for this repository:

* di-ipv-cri-address-build - configured for the lambda deployments.
* di-ipv-cri-address-infra-build - configured for the infrastructure deployments.

Both environments require the same secrets but different values according to the stack.

GitHub secrets are required which must be configured in an environment for security reasons.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| ARTIFACT_SOURCE_BUCKET_NAME | Upload artifact bucket |
| GH_ACTIONS_ROLE_ARN | Assumed role IAM ARN |
| SIGNING_PROFILE_NAME | Signing profile name |
