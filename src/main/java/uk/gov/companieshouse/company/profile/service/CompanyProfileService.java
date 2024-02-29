package uk.gov.companieshouse.company.profile.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.logging.Logger;


@Service
public class CompanyProfileService {

    private final Logger logger;
    private final CompanyProfileRepository companyProfileRepository;
    private final MongoTemplate mongoTemplate;
    private final CompanyProfileApiService companyProfileApiService;
    private final LinkRequestFactory linkRequestFactory;
    private final CompanyProfileTransformer companyProfileTransformer;

    /**
     * Constructor.
     */
    @Autowired
    public CompanyProfileService(Logger logger,
                                 CompanyProfileRepository companyProfileRepository,
                                 MongoTemplate mongoTemplate,
                                 CompanyProfileApiService companyProfileApiService,
                                 LinkRequestFactory linkRequestFactory,
                                 CompanyProfileTransformer companyProfileTransformer) {
        this.logger = logger;
        this.companyProfileRepository = companyProfileRepository;
        this.mongoTemplate = mongoTemplate;
        this.companyProfileApiService = companyProfileApiService;
        this.linkRequestFactory = linkRequestFactory;
        this.companyProfileTransformer = companyProfileTransformer;
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
            determineCanFile(companyNumber);
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

    private void addLink(LinkRequest linkRequest) {
        try {
            Query query = new Query(Criteria.where("_id").is(linkRequest.getCompanyNumber()));
            Update update = Update.update(
                    String.format("data.links.%s", convertToDBformat(linkRequest.getLinkType())),
                    String.format("/company/%s/%s", linkRequest.getCompanyNumber(),
                            linkRequest.getLinkType()));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(linkRequest.getContextId()));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.info(String.format("Company %s link inserted in Company Profile "
                            + "with context id: %s and company number: %s",
                    linkRequest.getLinkType(), linkRequest.getContextId(),
                    linkRequest.getCompanyNumber()));

            companyProfileApiService.invokeChsKafkaApi(linkRequest.getContextId(),
                    linkRequest.getCompanyNumber());
            logger.info(String.format("chs-kafka-api CHANGED invoked successfully for context "
                            + "id: %s and company number: %s", linkRequest.getContextId(),
                    linkRequest.getCompanyNumber()));
        } catch (IllegalArgumentException | ApiErrorResponseException exception) {
            logger.error("Error calling chs-kafka-api");
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            logger.error("Error accessing MongoDB");
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private void deleteLink(LinkRequest linkRequest) {
        try {
            Update update = new Update();
            update.unset(String.format("data.links.%s",
                    convertToDBformat(linkRequest.getLinkType())));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(linkRequest.getContextId()));
            Query query = new Query(Criteria.where("_id").is(linkRequest.getCompanyNumber()));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.info(String.format("Company %s link deleted in Company Profile "
                            + "with context id: %s and company number: %s",
                    linkRequest.getLinkType(), linkRequest.getContextId(),
                    linkRequest.getCompanyNumber()));

            companyProfileApiService.invokeChsKafkaApi(linkRequest.getContextId(),
                    linkRequest.getCompanyNumber());
            logger.info(String.format("chs-kafka-api DELETED invoked successfully for context "
                            + "id: %s and company number: %s", linkRequest.getContextId(),
                    linkRequest.getCompanyNumber()));
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
     * func creates a Link Request for a given link type
     * and calls checkAdd or checkDelete.
     */
    public void processLinkRequest(String linkType, String companyNumber, String contextId,
                                   boolean delete) {
        LinkRequest linkRequest =
                linkRequestFactory.createLinkRequest(linkType, contextId, companyNumber);

        if (delete) {
            checkForDeleteLink(linkRequest);
        } else {
            checkForAddLink(linkRequest);
        }
    }

    /**
     * Checks if link for given type exists in document and
     * call addLink if this is false.
     */
    public void checkForAddLink(LinkRequest linkRequest) {
        Data data = Optional.ofNullable(getDocument(linkRequest.getCompanyNumber()))
                    .map(CompanyProfileDocument::getCompanyProfile).orElseThrow(() ->
                            new ResourceNotFoundException(HttpStatus.NOT_FOUND,
                                    "no data for company profile: "
                                    + linkRequest.getCompanyNumber()));
        Links links = Optional.ofNullable(data.getLinks()).orElse(new Links());
        String linkData = linkRequest.getCheckLink().apply(links);

        if (!StringUtils.isBlank(linkData)) {
            logger.error(linkRequest.getLinkType() + " link for company profile already exists");
            throw new ResourceStateConflictException("Resource state conflict; "
                    + linkRequest.getLinkType() + " link already exists");
        } else {
            addLink(linkRequest);
        }
    }

    /**
     * Checks if link for given type does not exist in document and
     * call deleteLink if this is false.
     */
    public void checkForDeleteLink(LinkRequest linkRequest) {
        Data data = Optional.ofNullable(getDocument(linkRequest.getCompanyNumber()))
                .map(CompanyProfileDocument::getCompanyProfile).orElseThrow(() ->
                        new ResourceNotFoundException(HttpStatus.NOT_FOUND,
                                "no data for company profile: "
                                + linkRequest.getCompanyNumber()));
        Links links = Optional.ofNullable(data.getLinks()).orElseThrow(() ->
                new ResourceStateConflictException("links data not found"));
        String linkData = linkRequest.getCheckLink().apply(links);

        if (StringUtils.isBlank(linkData)) {
            logger.error(linkRequest.getLinkType() + " link for company profile already"
                    + " does not exist");
            throw new ResourceStateConflictException("Resource state conflict; "
                    + linkRequest.getLinkType() + " link already does not exist");
        } else {
            deleteLink(linkRequest);
        }
    }

    /**
     * finds existing company profile from db if any and
     * updates or saves new record into db.
     */
    public void processCompanyProfile(String contextId, String companyNumber,
                                      CompanyProfile companyProfile) {
        Optional<CompanyProfileDocument> existingProfile =
                companyProfileRepository.findById(companyNumber);
        Optional<Links> existingLinks = existingProfile
                .map(CompanyProfileDocument::getCompanyProfile)
                .map(Data::getLinks);

        CompanyProfileDocument companyProfileDocument = companyProfileTransformer
                .transform(companyProfile, companyNumber, existingLinks.orElse(null));

        try {
            companyProfileRepository.save(companyProfileDocument);
            logger.info(String.format("Company profile is updated in MongoDb for"
                            + " context id: %s and company number: %s", contextId, companyNumber));
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException("Saving to MongoDb failed", illegalArgumentEx);
        }

    }

    public Data retrieveCompanyNumber(String companyNumber)
            throws JsonProcessingException, ResourceNotFoundException {
        CompanyProfileDocument companyProfileDocument = getCompanyProfileDocument(companyNumber);
        return companyProfileDocument.getCompanyProfile();
    }

    private CompanyProfileDocument getCompanyProfileDocument(String companyNumber)
            throws ResourceNotFoundException {
        Optional<CompanyProfileDocument> companyProfileOptional =
                companyProfileRepository.findById(companyNumber);
        return companyProfileOptional.orElseThrow(() ->
                new ResourceNotFoundException(HttpStatus.NOT_FOUND, String.format(
                        "Resource not found for company number: %s", companyNumber)));
    }

    /** Delete company profile. */
    @Transactional
    public void deleteCompanyProfile(String companyNumber) throws ResourceNotFoundException {
        CompanyProfileDocument companyProfileDocument = getCompanyProfileDocument(companyNumber);

        companyProfileRepository.delete(companyProfileDocument);
        logger.info(String.format("Company profile is deleted in MongoDb with companyNumber %s",
                companyNumber));


    }

    /** Get company details. */
    public Optional<CompanyDetails> getCompanyDetails(String companyNumber)
            throws JsonProcessingException {
        try {
            Data companyProfile = retrieveCompanyNumber(companyNumber);
            CompanyDetails companyDetails = new CompanyDetails();
            companyDetails.setCompanyName(companyProfile.getCompanyName());
            companyDetails.setCompanyNumber(companyProfile.getCompanyNumber());
            companyDetails.setCompanyStatus(companyProfile.getCompanyStatus());
            return Optional.of(companyDetails);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            logger.error(resourceNotFoundException.getMessage());
            return Optional.empty();
        }

    }


    /** Set can_file based on company type and status. */
    public void determineCanFile(String companyNumber) {
        try {
            CompanyProfileDocument companyProfileDocument =
                    getCompanyProfileDocument(companyNumber);
            String companyType = companyProfileDocument.getCompanyProfile().getType();
            String companyStatus = companyProfileDocument.getCompanyProfile().getCompanyStatus();

            if (companyType.equals("ltd")
                    || companyType.equals("llp")
                    || companyType.equals("plc")
                    || companyType.contains("private")) {
                companyProfileDocument.getCompanyProfile()
                        .setCanFile(!companyStatus.equals("dissolved")
                        && !companyStatus.equals("converted-closed")
                        && !companyStatus.equals("petition-to-restore-dissolved"));
            } else {
                companyProfileDocument.getCompanyProfile().setCanFile(false);
            }
        } catch (Exception exception) {
            logger.error("Error determining can file status " + exception.getMessage());

        }

    }
}
