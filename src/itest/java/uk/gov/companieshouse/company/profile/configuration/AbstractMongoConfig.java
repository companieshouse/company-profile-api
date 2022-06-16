package uk.gov.companieshouse.company.profile.configuration;

import java.time.Duration;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * Mongodb configuration runs on test container.
 */
public class AbstractMongoConfig {

    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
            DockerImageName.parse("mongo:4.0.10"));

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        mongoDBContainer.setWaitStrategy(Wait.defaultWaitStrategy()
                .withStartupTimeout(Duration.of(300, SECONDS)));
        registry.add("spring.data.mongodb.uri", (() -> mongoDBContainer.getReplicaSetUrl() +
                "?serverSelectionTimeoutMS=100&connectTimeoutMS=100"));

        mongoDBContainer.start();
    }
}
