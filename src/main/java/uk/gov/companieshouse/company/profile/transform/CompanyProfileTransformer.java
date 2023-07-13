package uk.gov.companieshouse.company.profile.transform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;

@Component
public class CompanyProfileTransformer {

    @Autowired
    CompanyProfileTransformer() {
    }

    /**
     * transforms links in accordance with existing links in db.
     */
    public CompanyProfileDocument transform(CompanyProfile companyProfile,
                                            String companyNumber, @Nullable Links existinglinks) {
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(companyNumber);
        companyProfileDocument.setCompanyProfile(companyProfile.getData());

        Links links;
        if (companyProfile.getData().getLinks() != null) {
            links = companyProfile.getData().getLinks();
        } else {
            links = existinglinks;
        }
        companyProfileDocument.getCompanyProfile().setLinks(links);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS");
        companyProfileDocument.setDeltaAt(LocalDateTime.parse(
                companyProfile.getDeltaAt(), dateTimeFormatter));
        companyProfileDocument.setHasMortgages(companyProfile.getHasMortgages());
        companyProfileDocument.setUpdated(new Updated().setAt(LocalDateTime.now()));
        return companyProfileDocument;
    }
}
