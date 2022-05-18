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
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Links;
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
     * Update insolvency and charges links in company profile.
     *
     * @param contextId             fetched from the headers using the key x-request-id
     * @param companyNumber         company number
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void updateInsolvencyLink(String contextId, String companyNumber,
                                     final CompanyProfile companyProfileRequest) {

        try {

            CompanyProfileDocument cpDocument =
                    companyProfileRepository.findById(companyNumber).get();

            Updated updated = cpDocument.getUpdated();
            if (updated != null) {
                updated.setAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            } else {
                updated = new Updated(LocalDateTime
                        .now().truncatedTo(ChronoUnit.SECONDS),
                        contextId, "company_delta");
            }

            Links existingLinks = cpDocument.getCompanyProfile().getLinks();
            if (existingLinks == null) {
                existingLinks = new Links();
            }
            Links newLinks = companyProfileRequest.getData().getLinks();
            if (newLinks == null) {
                newLinks = new Links();
            }

            Update update1 = new Update();
            update1 = updateLinks(update1, existingLinks.getInsolvency(), newLinks.getInsolvency(),
                    "data.links.insolvency");
            update1 = updateLinks(update1, existingLinks.getCharges(), newLinks.getCharges(),
                    "data.links.charges");

            update1.set("data.etag", GenerateEtagUtil.generateEtag())
                    .set("updated", updated);

            Query query1 = new Query(Criteria.where("_id").is(companyNumber));
            mongoTemplate.upsert(query1, update1, CompanyProfileDocument.class);

        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException(illegalArgumentEx.getMessage());
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        }
        logger.trace(String.format("Company profile is updated in MongoDB with contextId %s "
                + "and company number %s", contextId, companyNumber));

        insolvencyApiService.invokeChsKafkaApi(contextId, companyNumber);
        logger.info(String.format("Chs-kafka-api CHANGED invoked successfully for "
                + "contextId %s and company number %s", contextId, companyNumber));
    }

    private Update updateLinks(Update update, String oldLink, String newLink, String fieldName) {

        if (newLink == null) {
            update.unset(fieldName);
        } else {
            update.set(fieldName, newLink);
        }
        return update;
    }
}
