package uk.gov.companieshouse.company.profile.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_LINK_TYPE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
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
import uk.gov.companieshouse.api.company.Accounts;
import uk.gov.companieshouse.api.company.AnnualReturn;
import uk.gov.companieshouse.api.company.BranchCompanyDetails;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.ConfirmationStatement;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.company.NextAccounts;
import uk.gov.companieshouse.api.company.PreviousCompanyNames;
import uk.gov.companieshouse.api.company.RegisteredOfficeAddress;
import uk.gov.companieshouse.api.company.SelfLink;
import uk.gov.companieshouse.api.company.UkEstablishment;
import uk.gov.companieshouse.api.company.UkEstablishmentsList;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.DocumentNotFoundException;
import uk.gov.companieshouse.api.exception.ResourceStateConflictException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.CompanyProfileDocument;
import uk.gov.companieshouse.api.model.Updated;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.logging.Logger;

@Service
public class CompanyProfileService {
    private static final String RELATED_COMPANIES_KIND = "related-companies";
    private static final String COMPANY_SELF_LINK = "/company/%s";
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
                companyNumber), DataMapHolder.getLogMap());
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
                        String.format("Successfully retrieved company profile with company "
                                + "number %s", companyNumber), DataMapHolder.getLogMap()),
                () -> logger.trace(
                        String.format("No company profile with company number %s found",
                                companyNumber), DataMapHolder.getLogMap())
        );

        //Stored as 'care_of_name' in Mongo, returned as 'care_of' in the GET endpoint
        if (companyProfileDocument.isPresent()) {
            CompanyProfileDocument document = companyProfileDocument.get();
            RegisteredOfficeAddress roa = document.getCompanyProfile().getRegisteredOfficeAddress();
            if (roa != null) {
                if (roa.getCareOf() == null) {
                    roa.setCareOf(roa.getCareOfName());
                }
                roa.setCareOfName(null);
                companyProfileDocument = Optional.of(document);
            }
        }

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
                logger.infoContext(contextId, String.format("Chs-kafka-api CHANGED "
                        + "invoked successfully for company number %s", companyNumber),
                        DataMapHolder.getLogMap());
                updateSpecificFields(cpDocument);
                logger.infoContext(contextId, String.format("Company profile is updated "
                        + "in MongoDB with company number %s", companyNumber),
                        DataMapHolder.getLogMap());
            } else {
                logger.errorContext(contextId, String.format("Chs-kafka-api CHANGED NOT invoked "
                        + "successfully for company number %s. Response code %s.", companyNumber,
                        statusCode.value()), new Exception("Chs-kafka-api CHANGED NOT invoked"),
                        DataMapHolder.getLogMap());
            }
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        }
    }

    private void addLink(LinkRequest linkRequest) {
        String companyNumber = linkRequest.getCompanyNumber();
        String contextId = linkRequest.getContextId();
        String linkType = linkRequest.getLinkType();
        try {
            Update update = Update.update(
                    String.format("data.links.%s", convertToDBformat(linkType)),
                    String.format("/company/%s/%s", companyNumber, linkType));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(contextId));

            if (Objects.equals(linkRequest.getLinkType(), "charges")) {
                update.set("data.has_charges", true);
            }

            Query query = new Query(Criteria.where("_id").is(companyNumber));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.infoContext(contextId, String.format("Company %s link inserted "
                    + "in Company Profile with company number: %s", linkType, companyNumber),
                    DataMapHolder.getLogMap());

            companyProfileApiService.invokeChsKafkaApi(contextId, companyNumber);
            logger.infoContext(contextId, String.format("chs-kafka-api CHANGED invoked "
                    + "successfully for company number: %s", companyNumber),
                    DataMapHolder.getLogMap());
        } catch (IllegalArgumentException exception) {
            logger.errorContext(contextId, "Error calling chs-kafka-api", exception,
                    DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            logger.errorContext(contextId, "Error accessing MongoDB", exception,
                    DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private void deleteLink(LinkRequest linkRequest) {
        String companyNumber = linkRequest.getCompanyNumber();
        String contextId = linkRequest.getContextId();
        String linkType = linkRequest.getLinkType();

        if (UK_ESTABLISHMENTS_LINK_TYPE.equals(linkType)) {
            int ukEstablishmentsCount = retrieveUkEstablishments(companyNumber).getItems().size();
            if (ukEstablishmentsCount > 1) {
                logger.infoContext(contextId, String.format("Link not deleted, "
                                + "UK establishments still exists for: %s", companyNumber),
                        DataMapHolder.getLogMap());
                return;
            }
            logger.infoContext(contextId, String.format("Company: %s only has zero or "
                            + "one uk establishment, link to be deleted.", companyNumber),
                    DataMapHolder.getLogMap());
        }

        try {
            Update update = new Update();
            update.unset(String.format("data.links.%s", convertToDBformat(linkType)));
            update.set("data.etag", GenerateEtagUtil.generateEtag());
            update.set("updated", new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(contextId));
            Query query = new Query(Criteria.where("_id").is(companyNumber));

            mongoTemplate.updateFirst(query, update, CompanyProfileDocument.class);
            logger.infoContext(contextId, String.format("Company %s link deleted "
                    + "in Company Profile with company number: %s",
                    linkType, companyNumber), DataMapHolder.getLogMap());

            companyProfileApiService.invokeChsKafkaApi(contextId, companyNumber);
            logger.infoContext(contextId, String.format("chs-kafka-api DELETED invoked "
                    + "successfully for company number: %s", companyNumber),
                    DataMapHolder.getLogMap());
        } catch (IllegalArgumentException exception) {
            logger.errorContext(contextId, "Error calling chs-kafka-api", exception,
                    DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            logger.errorContext(contextId, "Error accessing MongoDB", exception,
                    DataMapHolder.getLogMap());
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
            logger.error("Error accessing MongoDB", DataMapHolder.getLogMap());
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
        setUpdateIfNotNullOtherwiseRemove(update, "data.links.registers",
                companyProfileDocument.getCompanyProfile().getLinks() != null
                        ? companyProfileDocument.getCompanyProfile().getLinks().getRegisters()
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

        if (!isBlank(linkData)) {
            logger.error(linkRequest.getLinkType() + " link for company profile already exists",
                    DataMapHolder.getLogMap());
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
                    + " does not exist", DataMapHolder.getLogMap());
            throw new ResourceStateConflictException("Resource state conflict; "
                    + linkRequest.getLinkType() + " link already does not exist");
        } else {
            deleteLink(linkRequest);
        }
    }

    /**
     * Finds existing company profile from db if any and
     * updates or saves new record into db.
     */
    @Transactional
    public void processCompanyProfile(String contextId, String companyNumber,
                                      CompanyProfile companyProfile)
            throws ServiceUnavailableException, BadRequestException {

        Optional<CompanyProfileDocument> existingProfile =
                companyProfileRepository.findById(companyNumber);
        Optional<Links> existingLinks = existingProfile
                .map(CompanyProfileDocument::getCompanyProfile)
                .map(Data::getLinks);

        Optional.ofNullable(companyProfile.getData()).map(Data::getBranchCompanyDetails)
                .map(BranchCompanyDetails::getParentCompanyNumber)
                .ifPresent(parentCompanyNumber -> {
                    LinkRequest ukEstablishmentLinkRequest =
                            new LinkRequest(contextId, parentCompanyNumber,
                                    UK_ESTABLISHMENTS_LINK_TYPE,
                                    UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);

                    try {
                        if (companyProfile.getData().getType().equals("uk-establishment")) {
                            Links links = companyProfile.getData().getLinks();
                            links.setOverseas(String.format("/company/%s", parentCompanyNumber));
                            companyProfile.getData().setLinks(links);
                            checkForAddLink(ukEstablishmentLinkRequest);
                        }
                    } catch (DocumentNotFoundException documentNotFoundException) {
                        // create parent company if not present
                        companyProfileRepository.save(
                                createParentCompanyDocument(parentCompanyNumber));
                        companyProfileApiService.invokeChsKafkaApi(
                                contextId, parentCompanyNumber);
                    } catch (ResourceStateConflictException resourceStateConflictException) {
                        logger.info("Parent company link already exists");
                    }

                });

        if (companyProfile.getData() != null) {
            if (companyProfile.getData().getHasCharges() == null) {
                existingProfile
                        .map(CompanyProfileDocument::getCompanyProfile)
                        .map(Data::getHasCharges).ifPresent(hasCharges ->
                                companyProfile.getData().setHasCharges(hasCharges));

                if (companyProfile.getData().getLinks() != null) {
                    boolean hasCharges = companyProfile.getData().getLinks().getCharges() != null;
                    companyProfile.getData().setHasCharges(hasCharges);
                }
            }

            if (companyProfile.getData().getHasBeenLiquidated() == null) {
                existingProfile
                        .map(CompanyProfileDocument::getCompanyProfile)
                        .map(Data::getHasBeenLiquidated).ifPresent(hasBeenLiquidated ->
                                companyProfile.getData().setHasBeenLiquidated(hasBeenLiquidated)
                    );
            }
        }

        CompanyProfileDocument companyProfileDocument = companyProfileTransformer
                .transform(companyProfile, companyNumber, existingLinks.orElse(null));

        try {
            companyProfileRepository.save(companyProfileDocument);
            companyProfileApiService.invokeChsKafkaApi(contextId, companyNumber);

            logger.infoContext(contextId, String.format("Company profile is updated in "
                    + "MongoDb for company number: %s", companyNumber), DataMapHolder.getLogMap());
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException("Saving to MongoDb failed", illegalArgumentEx);
        }
    }

    private CompanyProfileDocument createParentCompanyDocument(String parentCompanyNumber) {
        CompanyProfileDocument parentCompanyDocument = new CompanyProfileDocument();
        parentCompanyDocument.setId(parentCompanyNumber);
        parentCompanyDocument.setDeltaAt(LocalDateTime.now());
        Data parentCompanyData = new Data();
        Links parentCompanyLinks = new Links();
        String ukEstablishmentLink = String.format("/company/%s/uk-establishments",
                parentCompanyNumber);
        parentCompanyLinks.setUkEstablishments(ukEstablishmentLink);
        parentCompanyData.setLinks(parentCompanyLinks);
        parentCompanyDocument.setCompanyProfile(parentCompanyData);
        return parentCompanyDocument;
    }

    /** Retrieve company profile. */
    public Data retrieveCompanyNumber(String companyNumber)
            throws ResourceNotFoundException {
        CompanyProfileDocument companyProfileDocument = getCompanyProfileDocument(companyNumber);
        companyProfileDocument = determineCanFile(companyProfileDocument);
        companyProfileDocument = determineOverdue(companyProfileDocument);

        Data profileData = companyProfileDocument.getCompanyProfile();
        if (profileData != null) {
            //SuperSecureManagingOfficerCount should not be returned on a Get request
            profileData.setSuperSecureManagingOfficerCount(null);
            List<PreviousCompanyNames> previousCompanyNames = profileData.getPreviousCompanyNames();
            if (previousCompanyNames != null && previousCompanyNames.isEmpty()) {
                profileData.setPreviousCompanyNames(null);
            }
            LocalDate dissolutionDate = profileData.getDateOfDissolution();
            if (dissolutionDate != null) {
                profileData.setDateOfCessation(dissolutionDate);
                profileData.setDateOfDissolution(null);
            }

            //Stored as 'care_of_name' in Mongo, returned as 'care_of' in the GET endpoint
            RegisteredOfficeAddress roa = profileData.getRegisteredOfficeAddress();
            if (roa != null) {
                if (roa.getCareOf() == null) {
                    roa.setCareOf(roa.getCareOfName());
                }
                roa.setCareOfName(null);
            }
        }
        return profileData;
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
    public void deleteCompanyProfile(String contextId,
                                     String companyNumber) throws ResourceNotFoundException {
        CompanyProfileDocument companyProfileDocument = getCompanyProfileDocument(companyNumber);
        Data companyProfile = companyProfileDocument.getCompanyProfile();
        String parentCompanyNumber = companyProfileDocument.getParentCompanyNumber();
        if (parentCompanyNumber != null && companyProfile.getType().equals("uk-establishment")) {
            LinkRequest ukEstablishmentLinkRequest =
                    new LinkRequest(contextId, parentCompanyNumber,
                            UK_ESTABLISHMENTS_LINK_TYPE,
                            UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
            checkForDeleteLink(ukEstablishmentLinkRequest);
        }

        companyProfileRepository.delete(companyProfileDocument);
        companyProfileApiService.invokeChsKafkaApiWithDeleteEvent(contextId, companyNumber,
                companyProfile);

        logger.info(String.format("Company profile is deleted in MongoDb with companyNumber %s",
                companyNumber), DataMapHolder.getLogMap());

    }

    /** Get company details. */
    public Optional<CompanyDetails> getCompanyDetails(String companyNumber) {
        try {
            Data companyProfile = retrieveCompanyNumber(companyNumber);
            CompanyDetails companyDetails = new CompanyDetails();
            companyDetails.setCompanyName(companyProfile.getCompanyName());
            companyDetails.setCompanyNumber(companyProfile.getCompanyNumber());
            companyDetails.setCompanyStatus(companyProfile.getCompanyStatus());
            return Optional.of(companyDetails);
        } catch (ResourceNotFoundException resourceNotFoundException) {
            logger.error(resourceNotFoundException.getMessage(), DataMapHolder.getLogMap());
            return Optional.empty();
        }
    }

    /**
     * Retrieves a list of company profile documents.
     * And then maps them into a list of UK establishments.
     *
     * @param parentCompanyNumber the supplied parent company number
     * @return a list of uk establishments
     *
     * @throws ResourceNotFoundException when a company is not located
     */
    public UkEstablishmentsList getUkEstablishments(String parentCompanyNumber)
            throws ResourceNotFoundException {
        String numberFound = getCompanyProfileDocument(parentCompanyNumber).getId();
        return retrieveUkEstablishments(numberFound);
    }

    private UkEstablishmentsList retrieveUkEstablishments(String companyNumber) {
        List<UkEstablishment> ukEstablishments = companyProfileRepository
                .findAllByParentCompanyNumber(companyNumber)
                .stream().map(company -> {
                    UkEstablishment ukEstablishment = new UkEstablishment();
                    ukEstablishment.setCompanyName(company.getCompanyProfile().getCompanyName());
                    ukEstablishment.setCompanyNumber(company.getId());
                    ukEstablishment.setCompanyStatus(company.getCompanyProfile()
                            .getCompanyStatus());
                    ukEstablishment.setLocality(company.getCompanyProfile()
                            .getRegisteredOfficeAddress().getLocality());
                    SelfLink companySelfLink = new SelfLink();
                    companySelfLink.setCompany(String.format(COMPANY_SELF_LINK, company.getId()));
                    ukEstablishment.setLinks(companySelfLink);
                    return ukEstablishment;
                }).collect(Collectors.toList());

        UkEstablishmentsList ukEstablishmentsList = new UkEstablishmentsList();
        ukEstablishmentsList.setItems(ukEstablishments);
        ukEstablishmentsList.setKind(RELATED_COMPANIES_KIND);
        ukEstablishmentsList.setEtag(GenerateEtagUtil.generateEtag());
        Links parentCompanyLink = new Links();
        parentCompanyLink.setSelf(String.format(COMPANY_SELF_LINK, companyNumber));
        ukEstablishmentsList.setLinks(parentCompanyLink);

        return ukEstablishmentsList;
    }

    /** Set can_file based on company type and status. */
    public CompanyProfileDocument determineCanFile(CompanyProfileDocument companyProfileDocument) {
        Data companyProfile = companyProfileDocument.getCompanyProfile();
        try {
            String companyType = companyProfile.getType();
            String companyStatus = companyProfile.getCompanyStatus();

            if (companyType == null || companyStatus == null) {
                companyProfile.setCanFile(false);
            }   else if (companyType.equals("ltd")
                    || companyType.equals("llp")
                    || companyType.equals("plc")
                    || companyType.contains("private")) {
                companyProfile.setCanFile(!companyStatus.equals("dissolved")
                        && !companyStatus.equals("converted-closed")
                        && !companyStatus.equals("petition-to-restore-dissolved"));
            } else {
                companyProfile.setCanFile(false);
            }
        } catch (Exception exception) {
            logger.error("Error determining can file status " + exception.getMessage(),
                    DataMapHolder.getLogMap());
        }

        companyProfileDocument.setCompanyProfile(companyProfile);
        return companyProfileDocument;
    }

    /** Set overdue field based on next due confirmation statement,
     * next accounts, and annual return. */
    public CompanyProfileDocument determineOverdue(CompanyProfileDocument companyProfileDocument) {
        Data companyProfile = companyProfileDocument.getCompanyProfile();
        try {
            LocalDate currentDate = LocalDate.now();

            Optional.ofNullable(companyProfile.getConfirmationStatement())
                    .map(ConfirmationStatement::getNextDue)
                    .ifPresent(nextDue -> {
                        companyProfile.getConfirmationStatement()
                            .setOverdue(nextDue.isBefore(currentDate));
                    });

            Optional.ofNullable(companyProfile.getAccounts())
                    .map(Accounts::getNextAccounts)
                    .map(NextAccounts::getDueOn)
                    .ifPresent(nextDue -> {
                        companyProfile.getAccounts().getNextAccounts()
                                .setOverdue(nextDue.isBefore(currentDate));
                        companyProfile.getAccounts().setOverdue(nextDue.isBefore(currentDate));
                    });

            Optional.ofNullable(companyProfile.getAnnualReturn())
                    .map(AnnualReturn::getNextDue)
                    .ifPresent(nextDue -> {
                        companyProfile.getAnnualReturn()
                                .setOverdue(nextDue.isBefore(currentDate));
                    });
        } catch (Exception exception) {
            logger.error("Error determining overdue status " + exception.getMessage());
        }
        companyProfileDocument.setCompanyProfile(companyProfile);
        return companyProfileDocument;
    }
}
