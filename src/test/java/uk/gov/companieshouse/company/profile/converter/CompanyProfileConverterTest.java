package uk.gov.companieshouse.company.profile.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;

@SpringBootTest
class CompanyProfileConverterTest {
    String companyProfileData;

    @Autowired
    @Qualifier("mongoConverterMapper")
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() throws IOException {
        String inputPath = "example-bson-company-profile-data.json";
        companyProfileData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        ClassLoader.getSystemClassLoader().getResourceAsStream(inputPath))));
    }

    @Test
    void convert() throws IOException {
        Document companyProfileBson = Document.parse(companyProfileData);

        Data companyProfile = new CompanyProfileConverter(objectMapper).convert(companyProfileBson);

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
}