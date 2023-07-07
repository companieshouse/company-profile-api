package uk.gov.companieshouse.company.profile.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class TestHelper {

    public String createJsonCompanyProfilePayload() throws IOException{
        InputStreamReader exampleJsonPayload = new InputStreamReader(
                ClassLoader.getSystemClassLoader().getResourceAsStream("company-profile-example.json"));

        return FileCopyUtils.copyToString(exampleJsonPayload);
    }

    public CompanyProfile createCompanyProfileObject() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper
                .readValue(this.createJsonCompanyProfilePayload(), CompanyProfile.class);
    }

    public CompanyProfileDocument createCompanyProfileDocument() throws IOException {
        CompanyProfile companyProfile = this.createCompanyProfileObject();
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyProfile.getData());
        companyProfileDocument.setHasMortgages(companyProfile.getHasMortgages());
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        companyProfileDocument.setUpdated(new Updated()
                .setAt(LocalDateTime.now()));
        return companyProfileDocument;
    }

}
