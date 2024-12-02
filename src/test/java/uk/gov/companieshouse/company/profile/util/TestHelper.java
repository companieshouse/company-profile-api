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
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
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

    public VersionedCompanyProfileDocument createCompanyProfileDocument() throws IOException {
        CompanyProfile companyProfile = this.createCompanyProfileObject();
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument();
        companyProfileDocument.setCompanyProfile(companyProfile.getData());
        companyProfileDocument.setHasMortgages(companyProfile.getHasMortgages());
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        companyProfileDocument.version(1L);
        companyProfileDocument.setDeltaAt(LocalDateTime.parse("2024-11-28T19:38:43"));
        companyProfileDocument.setUpdated(new Updated()
                .setAt(LocalDateTime.now()));
        return companyProfileDocument;
    }

    public static Links createExistingLinks() {
        Links existingLinks = new Links();
        existingLinks.setInsolvency("/company/00010001/insolvency");
        existingLinks.setCharges("/company/00010001/charges");
        return existingLinks;
    }

    public static VersionedCompanyProfileDocument createExistingCompanyProfile()  {
        Data companyProfileData = new Data();
        companyProfileData.setLinks(createExistingLinks());
        VersionedCompanyProfileDocument existingCompanyProfileDocument = new VersionedCompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        existingCompanyProfileDocument.version(1L);
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

    public VersionedCompanyProfileDocument createUkEstablishmentTestInput(String companyNumber) {
        VersionedCompanyProfileDocument companyProfileDocument = new VersionedCompanyProfileDocument();
        companyProfileDocument.setId(companyNumber);
        Data data = new Data();
        data.setCompanyStatus("active");
        data.setCompanyName("ACME Ltd");
        RegisteredOfficeAddress registeredOfficeAddress = new RegisteredOfficeAddress();
        registeredOfficeAddress.setLocality("Wales");
        data.setRegisteredOfficeAddress(registeredOfficeAddress);
        companyProfileDocument.setCompanyProfile(data);
        companyProfileDocument.version(1L);
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

    public VersionedCompanyProfileDocument createCompanyProfileTypeUkEstablishment(String companyNumber) {
        Data companyProfileData = new Data();
        companyProfileData.setType("uk-establishment");
        VersionedCompanyProfileDocument existingCompanyProfileDocument = new VersionedCompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        existingCompanyProfileDocument.setId(companyNumber);
        existingCompanyProfileDocument.setParentCompanyNumber("FC123456");
        existingCompanyProfileDocument.version(1L);
        return existingCompanyProfileDocument;
    }

    public VersionedCompanyProfileDocument createParentCompanyProfile(String companyNumber) {
        Data companyProfileData = new Data();
        Links existingLinks = new Links();
        existingLinks.setUkEstablishments(String.format("/company/%s/uk-establishments", companyNumber));
        companyProfileData.setLinks(existingLinks);
        VersionedCompanyProfileDocument existingCompanyProfileDocument = new VersionedCompanyProfileDocument();
        existingCompanyProfileDocument.setCompanyProfile(companyProfileData);
        existingCompanyProfileDocument.setId(companyNumber);
        existingCompanyProfileDocument.version(0L);
        return existingCompanyProfileDocument;
    }
}
