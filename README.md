# `company-profile-api`

## Summary

The `company-profile-api` is a service that receives company profile deltas from
`company-profile-delta-consumer`. It transforms these deltas to a standardised structure and then:

* stores or deletes documents within the `company_profile collection` in MongoDB, and
* enqueues a resource changed message that triggers further downstream processing.

The service is implemented in Java 21 using Spring Boot 3.3

## System requirements

* [Git](https://git-scm.com/downloads)
* [Java](http://www.oracle.com/technetwork/java/javase/downloads)
* [Maven](https://maven.apache.org/download.cgi)
* [MongoDB](https://www.mongodb.com/)

## Building and Running Locally using Docker

1. Clone [Docker CHS Development](https://github.com/companieshouse/docker-chs-development) and
   follow the steps in the
   README.
2. Enable the following services using the command `chs-dev services enable <service>`.
    * `company-profile-api`

3. Boot up the services' containers on docker using tilt `chs-dev up`.

## Building the docker image

```bash
mvn compile jib:dockerBuild
```

## To make local changes

Development mode is available for this service
in [Docker CHS Development](https://github.com/companieshouse/docker-chs-development).

```bash
chs-dev development enable company-profile-api
```

This will clone the `company-profile-api` into the `./repositories` folder. Any changes to the
code, or resources will automatically trigger a rebuild and relaunch.

## Terraform ECS

### What does this code do?

The code present in this repository is used to define and deploy a dockerised container in AWS ECS.
This is done by calling a [module](https://github.com/companieshouse/terraform-modules/tree/main/aws/ecs) from
terraform-modules. Application specific attributes are injected and the service is then deployed using Terraform via the
CICD platform 'Concourse'.

| Application specific attributes | Value                                                                                                                                                                                                                                                              | Description                                         |
|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| **ECS Cluster**                 | public-data                                                                                                                                                                                                                                                        | ECS cluster (stack) the service belongs to          |
| **Load balancer**               | {env}-chs-apichgovuk <br> {env}-chs-apichgovuk-private                                                                                                                                                                                                             | The load balancer that sits in front of the service |
| **Concourse pipeline**          | [Pipeline link](https://ci-platform.companieshouse.gov.uk/teams/team-development/pipelines/filing-history-data-api) <br> [Pipeline code](https://github.com/companieshouse/ci-pipelines/blob/master/pipelines/ssplatform/team-development/filing-history-data-api) | Concourse pipeline link in shared services          |

### Contributing

- Please refer to
  the [ECS Development and Infrastructure Documentation](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/4390649858/Copy+of+ECS+Development+and+Infrastructure+Documentation+Updated)
  for detailed information on the infrastructure being deployed.

### Testing

- Ensure the terraform runner local plan executes without issues. For information on terraform runners please see
  the [Terraform Runner Quickstart guide](https://companieshouse.atlassian.net/wiki/spaces/DEVOPS/pages/1694236886/Terraform+Runner+Quickstart).
- If you encounter any issues or have questions, reach out to the team on the **#platform** Slack channel.

### Vault Configuration Updates

- Any secrets required for this service will be stored in Vault. For any updates to the Vault configuration, please
  consult with the **#platform** team and submit a workflow request.

### Useful Links

- [ECS service config dev repository](https://github.com/companieshouse/ecs-service-configs-dev)
- [ECS service config production repository](https://github.com/companieshouse/ecs-service-configs-production)