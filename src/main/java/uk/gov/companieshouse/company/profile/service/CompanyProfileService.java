package uk.gov.companieshouse.company.profile.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyProfileRepository companyProfileRepository;

    public CompanyProfileService(Logger logger, CompanyProfileRepository companyProfileRepository) {
        this.logger = logger;
        this.companyProfileRepository = companyProfileRepository;
    }

    /**
     * Retrieve a company profile using its company number.
     *
     * @param companyNumber the company number
     * @return a company profile if one with such a company number exists, otherwise an empty
     *     optional
     */
    public Optional<CompanyProfileDocument> get(String companyNumber) {
        logger.trace(String.format("DSND-374: GET company profile with number %s", companyNumber));
        Optional<CompanyProfileDocument> companyProfileDao = companyProfileRepository
                .findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(companyNumber);

        logger.trace(String.format("DSND-374: Company profile with number %s retrieved: %s",
                companyNumber, companyProfileDao));
        return companyProfileDao;
    }

    /**
     * Updated insolvency links in company profile.
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void updateInsolvencyLink(final CompanyProfile companyProfileRequest) {
        CompanyProfileDocument companyProfile = companyProfileRepository
                .findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(
                        companyProfileRequest.getData().getCompanyNumber()).get();
        String insolvencyLink = companyProfileRequest.getData().getLinks().getInsolvency();
        companyProfile.companyProfile.getData().getLinks().setInsolvency(insolvencyLink);
        companyProfileRepository.save(companyProfile);
        logger.trace(String.format("DSND-376: Insolvency links updated: %s",
                companyProfileRequest.toString()));
    }
}
