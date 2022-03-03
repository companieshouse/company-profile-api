package uk.gov.companieshouse.company.profile.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;
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

    // TODO Update with newly generated CompanyProfile object once DSND-524 is completed

    /**
     * Retrieve a company profile using its company number.
     *
     * @param companyNumber the company number
     * @return a company profile if one with such a company number exists, otherwise an empty
     *     optional
     */
    public Optional<CompanyProfileApi> get(String companyNumber) {
        logger.trace(String.format("DSND-374: GET company profile with number %s", companyNumber));
        Optional<CompanyProfileApi> companyProfile =
                companyProfileRepository.findCompanyProfileApiByCompanyNumber(companyNumber);

        logger.trace(String.format("DSND-374: Company profile with number %s retrieved: %s",
                companyNumber, companyProfile));
        return companyProfile;
    }
}
