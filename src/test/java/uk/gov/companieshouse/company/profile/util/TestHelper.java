package uk.gov.companieshouse.company.profile.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.company.SelfLink;
import uk.gov.companieshouse.api.company.UkEstablishment;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;

public class TestHelper {

    public final static String notFoundErrorString =
              "{\n"
            + "    \"errors\": [\n"
            + "        {\n"
            + "            \"type\": \"ch:service\",\n"
            + "            \"error\": \"company-profile-not-found\"\n"
            + "        }\n"
            + "    ]\n"
            + "}";

    public String createJsonCompanyProfilePayload() throws IOException {
        InputStreamReader exampleJsonPayload = new InputStreamReader(
                ClassLoader.getSystemClassLoader().getResourceAsStream("company-profile-example.json"));

        return FileCopyUtils.copyToString(exampleJsonPayload);
    }

    public CompanyProfile createCompanyProfileObject() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
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

    public Links createExistingLinks() {
        Links existingLinks = new Links();
        existingLinks.setInsolvency("/company/00010001/insolvency");
        existingLinks.setCharges("/company/00010001/charges");
        return existingLinks;
    }

    public CompanyProfileDocument createExistingCompanyProfile()  {
        Data companyProfileData = new Data();
        companyProfileData.setLinks(this.createExistingLinks());
        CompanyProfileDocument existingCompanyProfileDocument = new CompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        return existingCompanyProfileDocument;
    }

    public CompanyProfile createCompanyProfileWithoutLinks() {
        CompanyProfile companyProfileWithOutLinks = new CompanyProfile();
        Data data = new Data();
        data.setCompanyNumber("0123");
        companyProfileWithOutLinks.setData(data);
        companyProfileWithOutLinks.setDeltaAt("20210102030405123456");
        companyProfileWithOutLinks.setHasMortgages(true);
        companyProfileWithOutLinks.setParentCompanyNumber("FC123456");
        return companyProfileWithOutLinks;
    }

    public CompanyProfileDocument createUkEstablishmentTestInput(String companyNumber) {
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(companyNumber);
        Data data = new Data();
        data.setCompanyStatus("active");
        data.setCompanyName("ACME Ltd");
        RegisteredOfficeAddress registeredOfficeAddress = new RegisteredOfficeAddress();
        registeredOfficeAddress.setLocality("Wales");
        data.setRegisteredOfficeAddress(registeredOfficeAddress);
        companyProfileDocument.setCompanyProfile(data);
        return companyProfileDocument;
    }

    public UkEstablishment createUkEstablishmentTestOutput(String companyNumber) {
        UkEstablishment ukEstablishment = new UkEstablishment();
        SelfLink selfLink = new SelfLink().company("/company/" + companyNumber);
        ukEstablishment.setLinks(selfLink);
        ukEstablishment.setLocality("Wales");
        ukEstablishment.setCompanyName("ACME Ltd");
        ukEstablishment.setCompanyStatus("active");
        ukEstablishment.setCompanyNumber(companyNumber);

        return ukEstablishment;
    }

    public CompanyProfileDocument createCompanyProfileTypeUkEstablishment(String companyNumber) {
        Data companyProfileData = new Data();
        companyProfileData.setType("uk-establishment");
        CompanyProfileDocument existingCompanyProfileDocument = new CompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        existingCompanyProfileDocument.setId(companyNumber);
        existingCompanyProfileDocument.setParentCompanyNumber("FC123456");
        return existingCompanyProfileDocument;
    }

    public CompanyProfileDocument createParentCompanyProfile(String companyNumber) {
        Data companyProfileData = new Data();
        Links existingLinks = new Links();
        existingLinks.setUkEstablishments(String.format("/company/%s/uk-establishments", companyNumber));
        companyProfileData.setLinks(existingLinks);
        CompanyProfileDocument existingCompanyProfileDocument = new CompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        existingCompanyProfileDocument.setId(companyNumber);
        return existingCompanyProfileDocument;
    }
}
