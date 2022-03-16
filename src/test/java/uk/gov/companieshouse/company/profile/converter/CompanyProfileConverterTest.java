package uk.gov.companieshouse.company.profile.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.Data;

class CompanyProfileConverterTest {
    String companyProfileData;

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        assertThat(companyProfile).usingRecursiveComparison().isEqualTo(expectedData);
    }
}