package uk.gov.companieshouse.company.profile.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.api.InsolvencyApiService;
import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.company.profile.exception.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;


@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyProfileRepository companyProfileRepository;
    private final MongoTemplate mongoTemplate;
    private final InsolvencyApiService insolvencyApiService;

    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Constructor.
     */
    public CompanyProfileService(Logger logger,
                                 CompanyProfileRepository companyProfileRepository,
                                 MongoTemplate mongoTemplate,
                                 InsolvencyApiService insolvencyApiService) {
        this.logger = logger;
        this.companyProfileRepository = companyProfileRepository;
        this.mongoTemplate = mongoTemplate;
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
        Optional<CompanyProfileDocument> companyProfileDocument;
        try {
            companyProfileDocument = companyProfileRepository.findById(companyNumber);
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException(illegalArgumentEx.getMessage());
        }

        companyProfileDocument.ifPresentOrElse(
                companyProfile -> logger.trace(
                        String.format("DSND-374: Company profile with number %s retrieved: %s",
                                companyNumber, companyProfile.getCompanyProfile().toString())),
                () -> logger.trace(
                        String.format("DSND-374: Company profile with number %s not found",
                                companyNumber))
        );

        return companyProfileDocument;
    }

    /**
     * Updated insolvency links in company profile.
     *
     * @param contextId             fetched from the headers using the key x-request-id
     * @param companyNumber         company number
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void updateInsolvencyLink(String contextId, String companyNumber,
                                     final CompanyProfile companyProfileRequest) {

        Optional<CompanyProfileDocument> companyProfileDocumentOptional =
                companyProfileRepository.findById(companyNumber);

        CompanyProfileDocument cpDocument = companyProfileDocumentOptional.orElseThrow(
                () -> new IllegalArgumentException(String.format(
                        "Resource not found for company number: %s", companyNumber)));

        companyProfileRequest.getData().setEtag(GenerateEtagUtil.generateEtag());

        cpDocument.setCompanyProfile(companyProfileRequest.getData());

        if (cpDocument.getUpdated() != null) {
            cpDocument.getUpdated()
                    .setAt(LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS));
        } else {
            Updated updated = new Updated(LocalDateTime
                    .now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS),
                    contextId, "company_delta");
            cpDocument.setUpdated(updated);
        }

        try {
            companyProfileRepository.save(cpDocument);
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException(illegalArgumentEx.getMessage());
        }

        insolvencyApiService.invokeChsKafkaApi(contextId, companyNumber);
        logger.info(String.format("DSND-377: ChsKafka api invoked successfully for company "
                + "number %s", companyNumber));
    }
}
