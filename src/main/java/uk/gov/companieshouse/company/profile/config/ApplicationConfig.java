package uk.gov.companieshouse.company.profile.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.converter.EnumWriteConverter;
import uk.gov.companieshouse.api.filter.CustomCorsFilter;
import uk.gov.companieshouse.company.profile.auth.EricTokenAuthenticationFilter;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataReadConverter;
import uk.gov.companieshouse.company.profile.converter.CompanyProfileDataWriteConverter;
import uk.gov.companieshouse.company.profile.serialization.LocalDateDeSerializer;
import uk.gov.companieshouse.company.profile.serialization.LocalDateSerializer;
import uk.gov.companieshouse.environment.EnvironmentReader;
import uk.gov.companieshouse.environment.impl.EnvironmentReaderImpl;
import uk.gov.companieshouse.logging.Logger;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ApplicationConfig {

    @Bean
    EnvironmentReader environmentReader() {
        return new EnvironmentReaderImpl();
    }

    /**
     * Configure Http Security.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAt(new EricTokenAuthenticationFilter(),
                        BasicAuthenticationFilter.class)
                .addFilterBefore(new CustomCorsFilter(externalMethods()), CsrfFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * Configure Web Security.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/company-profile-api/healthcheck");
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        var objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(List.of(
                new CompanyProfileDataWriteConverter(objectMapper),
                new CompanyProfileDataReadConverter(objectMapper, Data.class),
                new EnumWriteConverter()));
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        var module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        objectMapper.registerModule(module);

        return objectMapper;
    }

    @Bean
    public List<String> externalMethods() {
        return Arrays.asList(HttpMethod.GET.name());
    }
}
