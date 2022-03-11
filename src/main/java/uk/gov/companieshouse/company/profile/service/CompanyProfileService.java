package uk.gov.companieshouse.company.profile.service;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.api.InsolvencyApiService;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyProfileRepository companyProfileRepository;
    private final InsolvencyApiService insolvencyApiService;

    /**
     * @param logger to log statements.
     * @param companyProfileRepository repository class to interact with mongodb.
     * @param insolvencyApiService service to call chs-kafka api.
     */
    public CompanyProfileService(Logger logger, CompanyProfileRepository companyProfileRepository,
                                 InsolvencyApiService insolvencyApiService) {
        this.logger = logger;
        this.companyProfileRepository = companyProfileRepository;
        this.insolvencyApiService = insolvencyApiService;
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
    public void updateInsolvencyLink(final CompanyProfile companyProfileRequest)
            throws NoSuchElementException {

        Optional<CompanyProfileDocument> companyProfileOptional = companyProfileRepository
                .findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(
                        companyProfileRequest.getData().getCompanyNumber());

        if (companyProfileOptional.isEmpty()) {
            throw new NoSuchElementException("Database entry not found");
        }

        CompanyProfileDocument companyProfile = companyProfileOptional.get();
        String insolvencyLink = companyProfileRequest.getData().getLinks().getInsolvency();
        companyProfile.companyProfile.getData().getLinks().setInsolvency(insolvencyLink);
        companyProfileRepository.save(companyProfile);
        logger.trace(String.format("DSND-376: Insolvency links updated: %s",
                companyProfileRequest.toString()));

        String companyNumber = companyProfileRequest.getData().getCompanyNumber();

        insolvencyApiService.invokeChsKafkaApi(companyNumber);

        logger.info(String.format("DSND-377: ChsKafka api invoked successfully for company "
                + "number %s", companyNumber));
    }
}
