package uk.gov.companieshouse.company.profile.service;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.exceptions.BadRequestException;
import uk.gov.companieshouse.company.profile.exceptions.DocumentNotFoundException;
import uk.gov.companieshouse.company.profile.exceptions.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.exceptions.ResourceStateConflictException;
import uk.gov.companieshouse.company.profile.exceptions.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.Updated;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;


@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyProfileRepository companyProfileRepository;
    private final MongoTemplate mongoTemplate;
    private final CompanyProfileApiService companyProfileApiService;
    static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Constructor.
     */
    public CompanyProfileService(Logger logger,
                                 CompanyProfileRepository companyProfileRepository,
                                 MongoTemplate mongoTemplate,
                                 CompanyProfileApiService companyProfileApiService) {
        this.logger = logger;
        this.companyProfileRepository = companyProfileRepository;
        this.mongoTemplate = mongoTemplate;
        this.companyProfileApiService = companyProfileApiService;
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
                                     final CompanyProfile companyProfileRequest)
            throws ApiErrorResponseException {

        try {

            Optional<CompanyProfileDocument> cpDocumentOptional =
                    companyProfileRepository.findById(companyNumber);

            var cpDocument = cpDocumentOptional.orElseThrow(() ->
                    new DocumentNotFoundException(
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

            ApiResponse<Void> response = companyProfileApiService.invokeChsKafkaApi(
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

    private void addLink(String link, String companyNumber, String contextId, String delta) {
        try {
            Query query = new Query(Criteria.where("_id").is(companyNumber));
            Update update = Update.update(
                    String.format("data.links.%s", convertToDBformat(link)),
                    String.format("/company/%s/%s", companyNumber,
                            link));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(delta)
                    .setBy(contextId));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.info(String.format("Company %s link inserted in Company Profile "
                            + "with context id: %s and company number: %s",
                    link, contextId, companyNumber));

            companyProfileApiService.invokeChsKafkaApi(contextId,
                    companyNumber);
            logger.info(String.format("chs-kafka-api CHANGED invoked successfully for context "
                            + "id: %s and company number: %s", contextId,
                    companyNumber));
        } catch (IllegalArgumentException | ApiErrorResponseException exception) {
            logger.error("Error calling chs-kafka-api");
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            logger.error("Error accessing MongoDB");
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private void deleteLink(String link, String companyNumber, String contextId, String delta) {
        try {
            Update update = new Update();
            update.unset(String.format("data.links.%s", convertToDBformat(link)));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(delta)
                    .setBy(contextId));
            Query query = new Query(Criteria.where("_id").is(companyNumber));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.info(String.format("Company %s link deleted in Company Profile "
                            + "with context id: %s and company number: %s",
                    link, contextId, companyNumber));

            companyProfileApiService.invokeChsKafkaApi(contextId,
                    companyNumber);
            logger.info(String.format("chs-kafka-api DELETED invoked successfully for context "
                            + "id: %s and company number: %s", contextId,
                    companyNumber));
        } catch (IllegalArgumentException | ApiErrorResponseException exception) {
            logger.error("Error calling chs-kafka-api");
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            logger.error("Error accessing MongoDB");
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private CompanyProfileDocument getDocument(String companyNumber) {
        try {
            return companyProfileRepository.findById(companyNumber)
                    .orElseThrow(() -> new DocumentNotFoundException(
                            String.format("No company profile with company number %s found",
                                    companyNumber)));
        } catch (DataAccessException exception) {
            logger.error("Error accessing MongoDB");
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private void updateSpecificFields(CompanyProfileDocument companyProfileDocument) {
        Update update = new Update();
        setUpdateIfNotNull(update, "data.etag",
                companyProfileDocument.getCompanyProfile().getEtag());
        setUpdateIfNotNull(update, "updated",
                companyProfileDocument.getUpdated());
        setUpdateIfNotNullOtherwiseRemove(update, "data.links.insolvency",
                companyProfileDocument.getCompanyProfile().getLinks() != null
                        ?
                        companyProfileDocument.getCompanyProfile().getLinks().getInsolvency()
                        : null);
        setUpdateIfNotNullOtherwiseRemove(update, "data.links.charges",
                companyProfileDocument.getCompanyProfile().getLinks() != null
                        ? companyProfileDocument.getCompanyProfile().getLinks().getCharges()
                        : null);
        setUpdateIfNotNullOtherwiseRemove(update, "data.has_insolvency_history",
                companyProfileDocument.getCompanyProfile().getHasInsolvencyHistory());
        setUpdateIfNotNullOtherwiseRemove(update, "data.has_charges",
                companyProfileDocument.getCompanyProfile().getHasCharges());
        Query query = new Query(Criteria.where("_id").is(companyProfileDocument.getId()));
        mongoTemplate.upsert(query, update, CompanyProfileDocument.class);
    }

    private void setUpdateIfNotNull(Update update, String key, Object object) {
        if (object != null) {
            update.set(key, object);
        }
    }

    private void setUpdateIfNotNullOtherwiseRemove(Update update, String key, Object object) {
        if (object != null) {
            update.set(key, object);
        } else {
            update.unset(key);
        }
    }

    /**
     * transforms link type string to format required to fetch from db.
     */
    private String convertToDBformat(String linkType) {
        return linkType.replace('-', '_');
    }

    /**
     * formats link type and concatenates delta string to it.
     */
    private String convertDeltaType(String linkType) {
        return linkType
                .replace(linkType.charAt(linkType.lastIndexOf(115)),'_')
                .concat("delta");
    }

    /**
     * func creates a Link Request for a given link type
     * and calls checkAdd or checkDelete.
     */
    public void processLinkRequest(String linkType, String companyNumber, String contextId,
                                   boolean delete)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String delta = convertDeltaType(linkType);

        if (delete) {
            checkForDeleteLink(linkType, companyNumber, contextId, delta);
        } else {
            checkForAddLink(linkType, companyNumber, contextId, delta);
        }
    }

    /**
     * func formats linkRequest type, applying caps to first char after '_'
     * or '-' and substring(0,1)
     * to invoke getLink for their respective type.
     */
    private String formatLinkType(String linkType) {
        while (linkType.contains("-")) {
            linkType = linkType.replaceFirst("-[a-z]",
                    String.valueOf(
                            Character.toUpperCase(linkType.charAt(linkType.indexOf("-") + 1))));
        }
        return linkType.substring(0, 1).toUpperCase() + linkType.substring(1);
    }


    /**
     * Checks if link for given type exists in document and
     * call addLink if this is false.
     */
    public void checkForAddLink(String link, String companyNumber,
                                String contextId, String delta)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Links links;
        try {
            links = Optional.ofNullable(getDocument(companyNumber))
                    .map(CompanyProfileDocument::getCompanyProfile)
                    .map(Data::getLinks).get();
        } catch (NullPointerException ex) {
            throw new ResourceNotFoundException("no data for company profile: "
                    + companyNumber, ex);
        }

        if (!StringUtils.isBlank(
                (String) links.getClass().getMethod("get" + formatLinkType(link))
                        .invoke(links))) {
            logger.error(link + " link for company profile already exists");
            throw new ResourceStateConflictException("Resource state conflict; "
                    + link + " link already exists");
        } else {
            addLink(link, companyNumber, contextId, delta);
        }
    }

    /**
     * Checks if link for given type does not exist in document and
     * call deleteLink if this is false.
     */
    public void checkForDeleteLink(String link, String companyNumber,
                                   String contextId, String delta)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Links links;
        try {
            links = Optional.ofNullable(getDocument(companyNumber))
                    .map(CompanyProfileDocument::getCompanyProfile)
                    .map(Data::getLinks).get();
        } catch (NullPointerException ex) {
            throw new ResourceNotFoundException("no data for company profile: "
                    + companyNumber, ex);
        }

        if (StringUtils.isBlank(
                (String) links.getClass().getMethod("get" + formatLinkType(link))
                        .invoke(links))) {
            logger.error(link + " link for company profile already"
                    + " does not exist");
            throw new ResourceStateConflictException("Resource state conflict; "
                    + link + " link already does not exist");
        } else {
            deleteLink(link, companyNumber, contextId, delta);
        }
    }
}
