package uk.gov.companieshouse.company.profile.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.fasterxml.jackson.databind.module.SimpleModule;

import java.time.LocalDate;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.company.profile.auth.EricTokenAuthenticationFilter;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataReadConverter;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataWriteConverter;
import uk.gov.companieshouse.company.profile.converter.EnumConverters;
import uk.gov.companieshouse.company.profile.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.company.profile.serialization.LocalDateSerializer;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;



@Configuration
@EnableWebSecurity
public class ApplicationConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private Logger logger;

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
     * Configure Http Security.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().disable()
                .csrf().disable()
                .formLogin().disable()
                .logout().disable()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .addFilterAt(new EricTokenAuthenticationFilter(logger),
                        BasicAuthenticationFilter.class)
                .authorizeRequests()
                .anyRequest().permitAll();
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
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }
}