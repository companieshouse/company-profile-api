# `company-profile-api`

## Summary

The `company-profile-api` is a service that receives company profile deltas from
`company-profile-delta-consumer`. It transforms these deltas to a standardised structure and then:

* stores or deletes documents within the `company_profile collection` in MongoDB, and
* enqueues a resource changed message that triggers further downstream processing.

Documents are available for retrieval via the services GET endpoints, described below.

The service is implemented in Java 21 using Spring Boot 3.3

## Endpoints

| Method | URL                                                  | Description                                                                                                                     |
|--------|------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| GET    | `/company/{company_number}`                            | Returns company information for a given company number                                                                          |
| GET    | `/company/{company_number}/links`                      | Returns company information for a given company number in a 'data' field                                                        |
| GET    | `/company/{company_number}/company-detail`             | Returns company details object for a given company number                                                                       |
| GET    | `/company/{company_number}/uk-establishments`          | Returns a list of uk establishments for a given parent company number                                                           |
| GET    | `/company/{company_number}/uk-establishments/addresses`| Returns a list of uk establishments addresses for a given parent company number |
| PUT    | `/company/{company_number}/internal`                   | Inserts or updates an existing company profile within the collection, includes checks for delta staleness and Mongo versioning. |
| PATCH  | `/company/{company_number}/links`                      | Updates a company insolvency, charges and registers links for a given company number                                            |
| PATCH  | `/company/{company_number}/links/{link_type}`          | Updates a company to add a link of a given type                                                                                 |
| PATCH  | `/company/{company_number}/links/{link_type}/delete`   | Deletes a link of a given type for a company                                                                                    |                                                                                                 
| DELETE | `/company/{company_number}/internal`                   | Deletes company information for a given company number, includes checks for delta staleness                                     |

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

3. Boot up the services' containers in docker-chs-development using `chs-dev up`.

### Building the docker image

```bash
mvn compile jib:dockerBuild
```

### To make local changes

Development mode is available for this service
in [Docker CHS Development](https://github.com/companieshouse/docker-chs-development).

```bash
chs-dev development enable company-profile-api
```

This will clone the `company-profile-api` into the `./repositories` folder. Any changes to the
code, or resources will automatically trigger a rebuild and relaunch.

## Environment Variables

| Variable                        | Description                                                                                              | Example (from docker-chs-development) |
|---------------------------------|----------------------------------------------------------------------------------------------------------|---------------------------------------|
| CHS_KAFKA_API_URL               | The URL which the chs-kafka-api is hosted on                                                             | http://api.chs.local:4001             |
| CHS_API_KEY                     | The client ID of an API key, with internal app privileges, to call chs-kafka-api with                    | abc123def456ghi789                    |
| SERVER_PORT                     | The port at which the service is hosted in ECS                                                           | 8080                                  |
| LOG_LEVEL                       | The level of log messages output to the logs                                                             | debug                                 |
| HUMAN_LOG                       | A boolean value to enable more readable log messages                                                     | 1                                     |
| MONGODB_URL                     | The URL which mongo is hosted on                                                                         | mongodb://mongo:27017/company_profile |
| MONGODB_COLLECTION              | The name of the collection containing company profile documents in mongodb                               | company_profile                       |
| COMPANY_PROFILE_COLLECTION_NAME | The name of the collection containing company profile documents in mongodb                               | company_profile                       |
| TRANSACTIONS_ENABLED            | Toggles the transaction property for DELETE requests to restore documents if call to chs-kafka-api fails | true                                  |


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