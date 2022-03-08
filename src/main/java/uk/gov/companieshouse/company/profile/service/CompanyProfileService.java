package uk.gov.companieshouse.company.profile.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.CompanyRepository;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyRepository companyRepository;

    public CompanyProfileService(Logger logger, CompanyRepository companyRepository) {
        this.logger = logger;
        this.companyRepository = companyRepository;
    }

    /**
     * Updated insolvency links in company profile.
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void update(final CompanyProfile companyProfileRequest) {
        CompanyProfileDao companyProfile = companyRepository.findByCompanyNumber(companyProfileRequest.getData().getCompanyNumber());
        String insolvencyLink = companyProfileRequest.getData().getLinks().getInsolvency();
        companyProfile.companyProfile.getData().getLinks().setInsolvency(insolvencyLink);
        companyRepository.save(companyProfile);
        logger.debug(String.format("Data saved in company_profile collection : %s",
                companyProfileRequest.toString()));
    }


    /**
     * Retrieve a company profile using its company number.
     *
     * @param companyNumber the company number
     * @return a company profile if one with such a company number exists, otherwise an empty
     *     optional
     */
    public Optional<CompanyProfileDao> get(String companyNumber) {
        logger.trace(String.format("DSND-374: GET company profile with number %s", companyNumber));
        Optional<CompanyProfileDao> companyProfileDao = companyRepository
                .findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(companyNumber);

        logger.trace(String.format("DSND-374: Company profile with number %s retrieved: %s",
                companyNumber, companyProfileDao));
        return companyProfileDao;
    }

}
