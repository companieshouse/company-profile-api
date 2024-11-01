package uk.gov.companieshouse.company.profile.transform;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.logging.Logger;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CompanyProfileTransformer {

    private final Logger logger;

    @Autowired
    CompanyProfileTransformer(Logger logger) {
        this.logger = logger;
    }

    /**
     * transforms links in accordance with existing links in db.
     */
    public VersionedCompanyProfileDocument transform(VersionedCompanyProfileDocument companyProfileDocument,
                                            CompanyProfile companyProfile,
                                            @Nullable Links existinglinks) {

        companyProfileDocument.setCompanyProfile(companyProfile.getData());
        if (companyProfile.getData() != null) {
            companyProfileDocument.getCompanyProfile().setEtag(GenerateEtagUtil.generateEtag());

            //Stored as 'care_of_name' in Mongo, returned as 'care_of' in the GET endpoint
            RegisteredOfficeAddress roa = companyProfile.getData().getRegisteredOfficeAddress();
            if (roa != null && roa.getCareOf() != null) {
                if (roa.getCareOfName() == null) {
                    roa.setCareOfName(roa.getCareOf());
                }
                roa.setCareOf(null);
            }
        }

        transformLinks(companyProfile, existinglinks, companyProfileDocument);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");
        if (companyProfile.getDeltaAt() != null) {
            companyProfileDocument.setDeltaAt(LocalDateTime.parse(
                    companyProfile.getDeltaAt(), dateTimeFormatter));
        }
        if (companyProfile.getParentCompanyNumber() != null) {
            companyProfileDocument.setParentCompanyNumber(companyProfile.getParentCompanyNumber());
        }
        if (companyProfile.getHasMortgages() != null) {
            companyProfileDocument.setHasMortgages(companyProfile.getHasMortgages());
        }
        companyProfileDocument.setUpdated(new Updated().setAt(LocalDateTime.now()));
        return companyProfileDocument;
    }

    private void transformLinks(CompanyProfile companyProfile, Links existinglinks,
                                CompanyProfileDocument companyProfileDocument) {
        Links links = new Links();
        if (companyProfile.getData().getLinks() != null) {
            // Iterating through each link in the Links class and calling the getter and setter
            for (Field linkField : Links.class.getDeclaredFields()) {
                String upperCamelCaseField = linkField.getName().substring(0, 1).toUpperCase()
                        + linkField.getName().substring(1);
                String getMethodName = "get" + upperCamelCaseField;
                String setMethodName = "set" + upperCamelCaseField;
                try {
                    Method getMethod = Links.class.getMethod(getMethodName);
                    Method setMethod = Links.class.getMethod(setMethodName, String.class);

                    String newLink = (String) getMethod.invoke(companyProfile.getData().getLinks());
                    if (newLink != null) {
                        setMethod.invoke(links, newLink);
                    } else {
                        if (existinglinks != null) {
                            String existingLink = (String) getMethod.invoke(existinglinks);
                            setMethod.invoke(links, existingLink);
                        }
                    }
                } catch (Exception ex) {
                    logger.error("Error with links reflection: " + ex.getMessage());
                }
            }
        } else {
            links = existinglinks;
        }
        companyProfileDocument.getCompanyProfile().setLinks(links);
    }
}
