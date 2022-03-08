package uk.gov.companieshouse.company.profile.service;

import com.google.gson.Gson;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.CompanyRepository;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;
import uk.gov.companieshouse.logging.Logger;

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
}
