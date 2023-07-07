package uk.gov.companieshouse.company.profile.transform;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.company.profile.mapper.LinksMapper;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;

@Component
public class CompanyProfileTransformer {

    private final LinksMapper linksMapper;

    @Autowired
    CompanyProfileTransformer(LinksMapper linksMapper) {
        this.linksMapper = linksMapper;
    }

    /**
     * transforms links in accordance with existing links in db.
     */
    public CompanyProfileDocument transform(CompanyProfile companyProfile,
                                            String companyNumber, Optional<Links> links) {
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument();
        companyProfileDocument.setId(companyNumber);
        companyProfileDocument.setCompanyProfile(companyProfile.getData());

        Links existingLink = links.orElse(new Links());
        linksMapper.mapLinks(existingLink, companyProfile.getData().getLinks());
        companyProfileDocument.getCompanyProfile().setLinks(existingLink);
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter
                .ofPattern("yyyyMMddHHmmssSSSSSS");

        companyProfileDocument.setDeltaAt(LocalDateTime.parse(companyProfile
                .getDeltaAt(), dateTimeFormatter));
        companyProfileDocument.setHasMortgages(companyProfile.getHasMortgages());
        companyProfileDocument.setUpdated(new Updated().setAt(LocalDateTime.now()));
        return companyProfileDocument;
    }
}
