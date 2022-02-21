# di-ipv-cri-address-api
Address Collector Credential Issuer API

## Build

> Ensure that you are using the java version specified in `.sdkmanrc`.

Build with `./gradlew clean build buildZip`

## Deploy to dev account

Build a sam config toml file once only by running:
`sam deploy -t deploy/template.yaml --guided`
Then run `gds aws <account> -- ./deploy.sh`


## Deploy to AWS lambda

Automated GitHub actions deployments to di-ipv-cri-dev have been enabled for this repository.

The automated deployments are triggered on a push to main after PR approval.

GitHub secrets are required which must be configured in an environment for security reasons.

Required GitHub secrets:

| Secret | Description |
| ------ | ----------- |
| AWS_ROLE_ARN | Assumed role IAM ARN |
| AWS_PROFILE_PATH | Parameter Store path to the signing profile versioned ARN |
| AWS_ROLE_SESSION | Assumed Role Session ID
