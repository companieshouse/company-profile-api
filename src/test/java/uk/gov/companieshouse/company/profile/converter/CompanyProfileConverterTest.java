package uk.gov.companieshouse.company.profile.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;

@SpringBootTest
class CompanyProfileConverterTest {
    String companyProfileData;

    @SpyBean(name = "mongoConverterMapper")
    private ObjectMapper objectMapper;

    private CompanyProfileConverter companyProfileConverter;

    @BeforeEach
    void setup() throws IOException {
        companyProfileConverter = new CompanyProfileConverter(objectMapper);
        String inputPath = "example-bson-company-profile-data.json";
        companyProfileData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(inputPath))));
    }

    @Test
    void convertSuccessfully() throws IOException {
        Document companyProfileBson = Document.parse(companyProfileData);

        Data companyProfile = companyProfileConverter.convert(companyProfileBson);

        String expectedDataPath = "company-profile-data-expected.json";
        String expectedCompanyProfileData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(expectedDataPath))));

        Data expectedData = objectMapper.readValue(expectedCompanyProfileData, Data.class);
        // assert that we're using the custom objectMapper
        assertThat(objectMapper.getDeserializationConfig().getDefaultPropertyInclusion())
                .isEqualTo(JsonInclude.Value.construct(
                        JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL));
        assertThat(companyProfile).usingRecursiveComparison().isEqualTo(expectedData);
    }

    @Test
    void convertUnsuccessfully() throws JsonProcessingException {
        Document companyProfileInvalidBson = Document.parse(companyProfileData);

        doThrow(JsonProcessingException.class).when(objectMapper).readValue(anyString(),
                eq(Data.class));

        assertThrows(RuntimeException.class,
                () -> new CompanyProfileConverter(objectMapper).convert(companyProfileInvalidBson));

    }
}