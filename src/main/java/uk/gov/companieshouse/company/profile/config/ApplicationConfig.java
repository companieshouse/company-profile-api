package uk.gov.companieshouse.company.profile.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.module.SimpleModule;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataReadConverter;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataWriteConverter;
import uk.gov.companieshouse.company.profile.converter.EnumConverters;
import uk.gov.companieshouse.company.profile.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.company.profile.serialization.LocalDateSerializer;
import uk.gov.companieshouse.company.profile.serialization.LocalDateTimeDeSerializer;
import uk.gov.companieshouse.company.profile.serialization.LocalDateTimeSerializer;
import uk.gov.companieshouse.company.profile.serialization.OffsetDateTimeDeSerializer;
import uk.gov.companieshouse.company.profile.serialization.OffsetDateTimeSerializer;
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


    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        ObjectMapper objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(List
                .of(new CompanyProfileDataWriteConverter(objectMapper),
                new CompanyProfileDataReadConverter(objectMapper),new EnumConverters.StringToEnum(),
                new EnumConverters.EnumToString()));
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
        module.addDeserializer(OffsetDateTime.class, new OffsetDateTimeDeSerializer());
        module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeSerializer());

        objectMapper.registerModule(module);

        objectMapper.registerModule(new JavaTimeModule());

        return objectMapper;
    }
}