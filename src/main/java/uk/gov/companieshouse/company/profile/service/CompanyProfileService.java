package uk.gov.companieshouse.company.profile.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.api.InsolvencyApiService;
import uk.gov.companieshouse.company.profile.exception.BadRequestException;
import uk.gov.companieshouse.company.profile.exception.DocumentGoneException;
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
     * @return a company profile if one with given company number exists, otherwise - empty optional
     */
    public Optional<CompanyProfileDocument> get(String companyNumber) {
        logger.trace(String.format("Call to retrieve company profile with company number %s",
                companyNumber));
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
                        String.format("Successfully retrieved company profile "
                                + "with company number %s", companyNumber)),
                () -> logger.trace(
                        String.format("No company profile with company number %s found",
                                companyNumber))
        );

        return companyProfileDocument;
    }

    /**
     * Update insolvency links in company profile.
     *
     * @param contextId             fetched from the headers using the key x-request-id
     * @param companyNumber         company number
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void updateInsolvencyLink(String contextId, String companyNumber,
                                     final CompanyProfile companyProfileRequest) {

        try {

            Optional<CompanyProfileDocument> cpDocumentOptional =
                    companyProfileRepository.findById(companyNumber);

            var cpDocument = cpDocumentOptional.orElseThrow(() ->
                    new DocumentGoneException(
                        String.format("No company profile with company number %s found",
                                companyNumber)));

            companyProfileRequest.getData().setEtag(GenerateEtagUtil.generateEtag());

            cpDocument.setCompanyProfile(companyProfileRequest.getData());

            if (cpDocument.getUpdated() != null) {
                cpDocument.getUpdated()
                        .setAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            } else {
                Updated updated = new Updated(LocalDateTime
                        .now().truncatedTo(ChronoUnit.SECONDS),
                        contextId, "company_delta");
                cpDocument.setUpdated(updated);
            }

            ApiResponse<Void> response = insolvencyApiService.invokeChsKafkaApi(
                    contextId, companyNumber);

            HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode());

            if (statusCode.is2xxSuccessful()) {
                logger.info(String.format("Chs-kafka-api CHANGED invoked successfully for "
                        + "contextId %s and company number %s", contextId, companyNumber));
                updateSpecificFields(cpDocument);
                logger.info(String.format("Company profile is updated in MongoDB with "
                        + "contextId %s and company number %s", contextId, companyNumber));
            } else {
                logger.error(String.format("Chs-kafka-api CHANGED NOT invoked successfully for "
                        + "contextId %s and company number %s. Response code %s.",
                        contextId, companyNumber, statusCode.value()));
            }
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        }

    }

    private void updateSpecificFields(CompanyProfileDocument companyProfileDocument) {
        Update update = new Update();
        setUpdateIfNotNull(update, "data.etag",
                companyProfileDocument.getCompanyProfile().getEtag());
        setUpdateIfNotNull(update, "updated",
                companyProfileDocument.getUpdated());
        setUpdateIfNotNull(update, "data.links.insolvency",
                companyProfileDocument.getCompanyProfile().getLinks().getInsolvency());
        setUpdateIfNotNull(update, "data.links.charges",
                companyProfileDocument.getCompanyProfile().getLinks().getCharges());
        setUpdateIfNotNull(update, "data.has_insolvency_history",
                companyProfileDocument.getCompanyProfile().getHasInsolvencyHistory());
        setUpdateIfNotNull(update, "data.has_charges",
                companyProfileDocument.getCompanyProfile().getHasCharges());
        Query query = new Query(Criteria.where("_id").is(companyProfileDocument.getId()));
        mongoTemplate.upsert(query, update, CompanyProfileDocument.class);
    }

    private Update setUpdateIfNotNull(Update update, String key, Object object) {
        if (object != null) {
            update.set(key, object);
        }
        return update;
    }
}
