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

Any time you wish to deploy, run:

`gds aws <account> -- ./deploy.sh`


## Deploy to AWS lambda

Automated GitHub actions deployments to di-ipv-cri-dev have been enabled for this repository.

The automated deployments are triggered on a push to main after PR approval.

GitHub secrets are required which must be configured in an environment for security reasons.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| AWS_ROLE_ARN | Assumed role IAM ARN |
| AWS_PROFILE_PATH | Parameter Store path to the signing profile versioned ARN |
| AWS_ROLE_SESSION | Assumed Role Session ID |
