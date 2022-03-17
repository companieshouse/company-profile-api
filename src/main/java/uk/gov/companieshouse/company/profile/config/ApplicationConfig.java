package uk.gov.companieshouse.company.profile.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Configuration
public class ApplicationConfig implements WebMvcConfigurer {
    @Bean
    EnvironmentReader environmentReader() {
        return new EnvironmentReaderImpl();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public InternalApiClient internalApiClient() {
        return ApiSdkManager.getPrivateSDK();
    }


    @Primary
    @Bean
    public ObjectMapper defaultObjectMapper() {
        return new Jackson2ObjectMapperBuilder().build();
    }

    /**
     * Create a custom mapper to use in Mongo reading/writing converters.
     * @return a custom object mapper
     */
    @Bean("mongoConverterMapper")
    public ObjectMapper customMapper() {
        ObjectMapper customMapper = new Jackson2ObjectMapperBuilder().build();
        // Exclude properties with null values from being serialised
        customMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return customMapper;
    }
}