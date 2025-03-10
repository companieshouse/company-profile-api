package uk.gov.companieshouse.company.profile.service;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;
import static uk.gov.companieshouse.company.profile.util.DateUtils.isDeltaStale;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_DELTA_TYPE;
import static uk.gov.companieshouse.company.profile.util.LinkRequest.UK_ESTABLISHMENTS_LINK_TYPE;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.company.Accounts;
import uk.gov.companieshouse.api.company.AnnualReturn;
import uk.gov.companieshouse.api.company.BranchCompanyDetails;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.ConfirmationStatement;
import uk.gov.companieshouse.api.company.CorporateAnnotation;
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
import uk.gov.companieshouse.company.profile.exception.ConflictException;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.company.profile.model.UnversionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.company.profile.transform.CompanyProfileTransformer;
import uk.gov.companieshouse.company.profile.util.LinkRequest;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class CompanyProfileService {

    private static final String RELATED_COMPANIES_KIND = "related-companies";
    private static final String COMPANY_SELF_LINK = "/company/%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);
    private static final String RESOURCE_NOT_FOUND_STRING = "Resource not found for company profile %s";

    private final CompanyProfileRepository companyProfileRepository;
    private final MongoTemplate mongoTemplate;
    private final CompanyProfileApiService companyProfileApiService;
    private final LinkRequestFactory linkRequestFactory;
    private final CompanyProfileTransformer companyProfileTransformer;

    /**
     * Constructor.
     */
    @Autowired
    public CompanyProfileService(CompanyProfileRepository companyProfileRepository,
            MongoTemplate mongoTemplate,
            CompanyProfileApiService companyProfileApiService,
            LinkRequestFactory linkRequestFactory,
            CompanyProfileTransformer companyProfileTransformer) {
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
     * @return a company profile if one with given company number exists, otherwise throw ResourceNotFoundException
     */
    public VersionedCompanyProfileDocument get(String companyNumber) throws ResourceNotFoundException {
        Optional<VersionedCompanyProfileDocument> companyProfileDocument;
        try {
            companyProfileDocument = companyProfileRepository.findById(companyNumber);
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException(illegalArgumentEx.getMessage());
        }

        //Stored as 'care_of_name' in Mongo, returned as 'care_of' in the GET endpoint
        if (companyProfileDocument.isPresent()) {
            VersionedCompanyProfileDocument document = companyProfileDocument.get();
            RegisteredOfficeAddress roa = document.getCompanyProfile().getRegisteredOfficeAddress();
            if (roa != null) {
                if (roa.getCareOf() == null) {
                    roa.setCareOf(roa.getCareOfName());
                }
                roa.setCareOfName(null);
            }
            return document;
        } else {
            throw new ResourceNotFoundException(
                    HttpStatusCode.valueOf(NOT_FOUND.value()),
                    String.format(RESOURCE_NOT_FOUND_STRING, companyNumber)
            );
        }
    }

    /**
     * Update insolvency links in company profile.
     *
     * @param companyNumber         company number
     * @param companyProfileRequest company Profile information {@link CompanyProfile}
     */
    public void updateInsolvencyLink(String companyNumber,
            final CompanyProfile companyProfileRequest) {
        try {

            Optional<VersionedCompanyProfileDocument> cpDocumentOptional =
                    companyProfileRepository.findById(companyNumber);

            VersionedCompanyProfileDocument cpDocument = cpDocumentOptional.orElseThrow(() ->
                    new DocumentNotFoundException(
                            String.format("Company profile %s not found", companyNumber)));

            Data data = Optional.of(companyProfileRequest)
                    .map(CompanyProfile::getData)
                    .orElseThrow(() -> new BadRequestException("No data in request body"));

            Links links = Optional.of(data)
                    .map(Data::getLinks)
                    .orElseThrow(() -> new BadRequestException("No links in request body"));

            cpDocument.setHasMortgages(isNotBlank(links.getCharges()));
            Data existingData = cpDocument.getCompanyProfile();
            existingData.setEtag(GenerateEtagUtil.generateEtag());

            Links existingLinks = Optional.ofNullable(existingData.getLinks()).orElse(new Links());
            existingLinks.setInsolvency(links.getInsolvency());
            existingLinks.setCharges(links.getCharges());
            existingLinks.setRegisters(links.getRegisters());

            existingData.setLinks(existingLinks);
            existingData.setHasInsolvencyHistory(data.getHasInsolvencyHistory());
            existingData.setHasCharges(data.getHasCharges());

            cpDocument.setCompanyProfile(existingData);

            if (cpDocument.getUpdated() != null) {
                cpDocument.getUpdated()
                        .setAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            } else {
                Updated updated = new Updated(LocalDateTime
                        .now().truncatedTo(ChronoUnit.SECONDS),
                        DataMapHolder.getRequestId(), "company_delta");
                cpDocument.setUpdated(updated);
            }

            ApiResponse<Void> response = companyProfileApiService.invokeChsKafkaApi(companyNumber);

            HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode());

            if (statusCode.is2xxSuccessful()) {
                if (cpDocument.getVersion() == null) { // Update a legacy document
                    mongoTemplate.save(new UnversionedCompanyProfileDocument(cpDocument));
                } else { // Update a versioned document
                    companyProfileRepository.save(cpDocument);
                }
                LOGGER.info("Company profile is updated in MongoDB", DataMapHolder.getLogMap());
            } else {
                LOGGER.error(String.format("Chs-kafka-api CHANGED invocation failed. Response code %s.",
                                statusCode.value()), DataMapHolder.getLogMap());
            }
        } catch (DataAccessException dbException) {
            throw new ServiceUnavailableException(dbException.getMessage());
        }
    }

    /**
     * func creates a Link Request for a given link type and calls checkAdd or checkDelete.
     */
    public void processLinkRequest(String linkType, String companyNumber, boolean delete) {
        LinkRequest linkRequest = linkRequestFactory.createLinkRequest(linkType, companyNumber);

        if (delete) {
            checkForDeleteLink(linkRequest);
        } else {
            checkForAddLink(linkRequest);
        }
    }

    /**
     * Checks if link for given type exists in document and call addLink if this is false.
     */
    public void checkForAddLink(LinkRequest linkRequest) {
        VersionedCompanyProfileDocument existingDocument = getDocument(linkRequest.getCompanyNumber());
        Data data = Optional.of(existingDocument)
                .map(CompanyProfileDocument::getCompanyProfile).orElseThrow(() ->
                        new ResourceNotFoundException(HttpStatus.NOT_FOUND,
                                RESOURCE_NOT_FOUND_STRING.formatted(linkRequest.getCompanyNumber())));
        Links links = Optional.ofNullable(data.getLinks()).orElse(new Links());
        String linkData = linkRequest.getCheckLink().apply(links);

        if (!isBlank(linkData)) {
            throw new ResourceStateConflictException(
                    "Resource state conflict; %s link already exists".formatted(linkRequest.getLinkType()));
        } else {
            addLink(linkRequest, existingDocument);
        }
    }

    /**
     * Checks if link for given type does not exist in document and call deleteLink if this is false.
     */
    public void checkForDeleteLink(LinkRequest linkRequest) {
        VersionedCompanyProfileDocument existingDocument = getDocument(linkRequest.getCompanyNumber());
        Data data = Optional.of(existingDocument)
                .map(CompanyProfileDocument::getCompanyProfile).orElseThrow(() ->
                        new ResourceNotFoundException(HttpStatus.NOT_FOUND,
                                RESOURCE_NOT_FOUND_STRING.formatted(linkRequest.getCompanyNumber())));
        Links links = Optional.ofNullable(data.getLinks()).orElseThrow(() ->
                new ResourceStateConflictException("No links exist for this company profile"));
        String linkData = linkRequest.getCheckLink().apply(links);

        if (isBlank(linkData)) {
            throw new ResourceStateConflictException(
                    "Resource state conflict; %s link already does not exist".formatted(linkRequest.getLinkType()));
        } else {
            deleteLink(linkRequest, existingDocument);
        }
    }

    public void checkForDeleteLinkUkEstablishmentParent(LinkRequest linkRequest) {
        Optional<VersionedCompanyProfileDocument> existingDocumentOptional =
                companyProfileRepository.findById(linkRequest.getCompanyNumber());

        if (existingDocumentOptional.isPresent()) {
            Data data = existingDocumentOptional
                    .map(CompanyProfileDocument::getCompanyProfile).orElseThrow(() ->
                            new ResourceNotFoundException(HttpStatus.NOT_FOUND,
                                    RESOURCE_NOT_FOUND_STRING.formatted(linkRequest.getCompanyNumber())));
            Links links = Optional.ofNullable(data.getLinks()).orElseThrow(() ->
                    new ResourceStateConflictException("links data not found"));
            String linkData = linkRequest.getCheckLink().apply(links);

            if (isBlank(linkData)) {
                throw new ResourceStateConflictException(
                        "Resource state conflict; %s link already does not exist".formatted(linkRequest.getLinkType()));
            } else {
                deleteLink(linkRequest, existingDocumentOptional.get());
            }
        } else {
            LOGGER.info("No document found for parent profile, continuing to delete child Uk establishment",
                    DataMapHolder.getLogMap());
        }
    }

    /**
     * Finds existing company profile from db if any and updates or saves new record into db.
     */
    public void processCompanyProfile(String companyNumber, CompanyProfile companyProfile)
            throws ServiceUnavailableException, BadRequestException {

        VersionedCompanyProfileDocument companyProfileDocument =
                companyProfileRepository.findById(companyNumber)
                        .orElse(new VersionedCompanyProfileDocument());

        deltaAtCheck(companyProfile.getDeltaAt(), companyProfileDocument.getDeltaAt());

        Optional<Links> existingLinks = Optional.of(companyProfileDocument)
                .map(CompanyProfileDocument::getCompanyProfile)
                .map(Data::getLinks);

        Optional.ofNullable(companyProfile.getData()).map(Data::getBranchCompanyDetails)
                .map(BranchCompanyDetails::getParentCompanyNumber)
                .ifPresent(parentCompanyNumber -> {
                    LinkRequest ukEstablishmentLinkRequest =
                            new LinkRequest(DataMapHolder.getRequestId(), parentCompanyNumber,
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
                        LOGGER.info("Creating new parent company document", DataMapHolder.getLogMap());
                        companyProfileRepository.insert(
                                createParentCompanyDocument(parentCompanyNumber));
                        companyProfileApiService.invokeChsKafkaApi(parentCompanyNumber);
                    } catch (ResourceStateConflictException resourceStateConflictException) {
                        LOGGER.info("Parent company link already exists", DataMapHolder.getLogMap());
                    }

                });

        if (companyProfile.getData() != null) {
            if (companyProfile.getData().getHasCharges() == null) {
                Optional.of(companyProfileDocument)
                        .map(CompanyProfileDocument::getCompanyProfile)
                        .map(Data::getHasCharges).ifPresent(hasCharges -> {
                            companyProfile.getData().setHasCharges(hasCharges);
                            companyProfile.setHasMortgages(hasCharges);
                        });

                if (companyProfile.getData().getLinks() != null) {
                    boolean hasCharges = companyProfile.getData().getLinks().getCharges() != null;
                    companyProfile.getData().setHasCharges(hasCharges);
                    companyProfile.setHasMortgages(hasCharges);
                }
            }

            if (companyProfile.getData().getHasBeenLiquidated() == null) {
                Optional.of(companyProfileDocument)
                        .map(CompanyProfileDocument::getCompanyProfile)
                        .map(Data::getHasBeenLiquidated).ifPresent(hasBeenLiquidated ->
                                companyProfile.getData().setHasBeenLiquidated(hasBeenLiquidated)
                        );
            }
        }

        VersionedCompanyProfileDocument transformedDocument = companyProfileTransformer
                .transform(companyProfileDocument, companyProfile, existingLinks.orElse(null));

        try {
            if (companyProfileDocument.getId() == null) { // A new document
                transformedDocument.setId(companyNumber);
                companyProfileRepository.insert(transformedDocument);
            } else if (companyProfileDocument.getVersion() == null) { // A legacy document
                mongoTemplate.save(new UnversionedCompanyProfileDocument(transformedDocument));
            } else {
                companyProfileRepository.save(transformedDocument);
            }
            companyProfileApiService.invokeChsKafkaApi(companyNumber);

            LOGGER.info(String.format("Company profile is updated in MongoDb for company number: %s", companyNumber),
                    DataMapHolder.getLogMap());
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException("Saving to MongoDb failed", illegalArgumentEx);
        }
    }

    /**
     * Retrieve company profile.
     */
    public Data retrieveCompanyNumber(String companyNumber)
            throws ResourceNotFoundException {
        VersionedCompanyProfileDocument companyProfileDocument = getCompanyProfileDocument(companyNumber);
        companyProfileDocument = determineCanFile(companyProfileDocument);
        companyProfileDocument = determineOverdue(companyProfileDocument);

        Data profileData = companyProfileDocument.getCompanyProfile();
        if (profileData != null) {
            // Do not output proof status
            profileData.setProofStatus(null);

            //SuperSecureManagingOfficerCount should not be returned on a Get request
            profileData.setSuperSecureManagingOfficerCount(null);
            List<PreviousCompanyNames> previousCompanyNames = profileData.getPreviousCompanyNames();
            if (previousCompanyNames != null && previousCompanyNames.isEmpty()) {
                profileData.setPreviousCompanyNames(null);
            }
            List<CorporateAnnotation> corporateAnnotations = profileData.getCorporateAnnotation();
            if (corporateAnnotations != null && corporateAnnotations.isEmpty()) {
                profileData.setCorporateAnnotation(null);
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


    /**
     * Delete company profile.
     */
    public void deleteCompanyProfile(String companyNumber, String requestDeltaAt) {
        if (StringUtils.isBlank(requestDeltaAt)) {
            throw new BadRequestException("delta_at is missing from delete request");
        }
        companyProfileRepository.findById(companyNumber).ifPresentOrElse(
                companyProfileDocument -> {
                    Data companyProfile = companyProfileDocument.getCompanyProfile();
                    LocalDateTime existingDeltaAt = companyProfileDocument.getDeltaAt();

                    deltaAtCheck(requestDeltaAt, existingDeltaAt);

                    String parentCompanyNumber = companyProfileDocument.getParentCompanyNumber();
                    if (parentCompanyNumber != null && companyProfile.getType().equals("uk-establishment")) {
                        LinkRequest ukEstablishmentLinkRequest =
                                new LinkRequest(DataMapHolder.getRequestId(), parentCompanyNumber,
                                        UK_ESTABLISHMENTS_LINK_TYPE,
                                        UK_ESTABLISHMENTS_DELTA_TYPE, Links::getUkEstablishments);
                        // Business logic states UK establishments need deletion even if the parent document is not present
                        checkForDeleteLinkUkEstablishmentParent(ukEstablishmentLinkRequest);
                    }

                    companyProfileRepository.delete(companyProfileDocument);
                    LOGGER.info("Company profile is deleted in MongoDb successfully", DataMapHolder.getLogMap());
                    companyProfileApiService.invokeChsKafkaApiWithDeleteEvent(companyNumber, companyProfile);
                },
                () -> {
                    LOGGER.info("Delete for non-existent document", DataMapHolder.getLogMap());
                    companyProfileApiService.invokeChsKafkaApiWithDeleteEvent(companyNumber, null);
                }
        );
    }

    /**
     * Get company details.
     */
    public CompanyDetails getCompanyDetails(String companyNumber) {
        Data companyProfile = retrieveCompanyNumber(companyNumber);

        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails.setCompanyName(companyProfile.getCompanyName());
        companyDetails.setCompanyNumber(companyProfile.getCompanyNumber());
        companyDetails.setCompanyStatus(companyProfile.getCompanyStatus());

        return companyDetails;
    }

    /**
     * Retrieves a list of company profile documents. And then maps them into a list of UK establishments.
     *
     * @param parentCompanyNumber the supplied parent company number
     * @return a list of uk establishments
     * @throws ResourceNotFoundException when a company is not located
     */
    public UkEstablishmentsList getUkEstablishments(String parentCompanyNumber)
            throws ResourceNotFoundException {
        String numberFound = getCompanyProfileDocument(parentCompanyNumber).getId();
        return retrieveUkEstablishments(numberFound);
    }


    /**
     * Set can_file based on company type and status.
     */
    public VersionedCompanyProfileDocument determineCanFile(VersionedCompanyProfileDocument companyProfileDocument) {
        Data companyProfile = companyProfileDocument.getCompanyProfile();
        try {
            String companyType = companyProfile.getType();
            String companyStatus = companyProfile.getCompanyStatus();

            if (companyType == null || companyStatus == null) {
                companyProfile.setCanFile(false);
            } else if (companyType.equals("ltd")
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
            LOGGER.error("Error determining can file status %s".formatted(exception.getMessage()),
                    DataMapHolder.getLogMap());
        }

        companyProfileDocument.setCompanyProfile(companyProfile);
        return companyProfileDocument;
    }

    /**
     * Set overdue field based on next due confirmation statement, next accounts, and annual return.
     */
    public VersionedCompanyProfileDocument determineOverdue(VersionedCompanyProfileDocument companyProfileDocument) {
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
            LOGGER.error("Error determining overdue status %s".formatted(exception.getMessage()), DataMapHolder.getLogMap());
        }
        companyProfileDocument.setCompanyProfile(companyProfile);
        return companyProfileDocument;
    }

    private void addLink(LinkRequest linkRequest, VersionedCompanyProfileDocument existingDocument) {
        try {
            Links links = Optional.ofNullable(existingDocument.getCompanyProfile().getLinks()).orElse(new Links());
            setLinksOnType(links, linkRequest.getLinkType(), linkRequest.getCompanyNumber());
            existingDocument.getCompanyProfile().setLinks(links);
            existingDocument.getCompanyProfile().setEtag(GenerateEtagUtil.generateEtag());
            existingDocument.setUpdated(new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(linkRequest.getContextId()));

            if (linkRequest.getLinkType().equals("charges")) {
                existingDocument.getCompanyProfile().setHasCharges(true);
                existingDocument.setHasMortgages(true);
            } else {
                existingDocument.setHasMortgages(false);
            }

            if (existingDocument.getVersion() == null) { // Update a legacy document with links
                mongoTemplate.save(new UnversionedCompanyProfileDocument(existingDocument));
            } else { // Update a versioned document with links
                companyProfileRepository.save(existingDocument);
            }
            LOGGER.info(String.format("Company %s link inserted in Company Profile", linkRequest.getLinkType()),
                    DataMapHolder.getLogMap());

            companyProfileApiService.invokeChsKafkaApi(linkRequest.getCompanyNumber());
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Error calling chs-kafka-api", exception, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            LOGGER.error("Error accessing MongoDB", exception, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private void deleteLink(LinkRequest linkRequest, VersionedCompanyProfileDocument existingDocument) {
        if (UK_ESTABLISHMENTS_LINK_TYPE.equals(linkRequest.getLinkType())) {
            int ukEstablishmentsCount = retrieveUkEstablishments(linkRequest.getCompanyNumber())
                    .getItems().size();
            if (ukEstablishmentsCount > 1) {
                LOGGER.info("Link not deleted, UK establishments still exists",
                        DataMapHolder.getLogMap());
                return;
            }
        }
        try {
            existingDocument.setHasMortgages(!linkRequest.getLinkType().equals("charges"));
            unsetLinksOnType(existingDocument.getCompanyProfile().getLinks(), linkRequest.getLinkType());
            existingDocument.getCompanyProfile().setEtag(GenerateEtagUtil.generateEtag());
            existingDocument.setUpdated(new Updated()
                    .setAt(LocalDateTime.now())
                    .setType(linkRequest.getDeltaType())
                    .setBy(linkRequest.getContextId()));

            if (existingDocument.getVersion() == null) { // Update a legacy document
                mongoTemplate.save(new UnversionedCompanyProfileDocument(existingDocument));
            } else { // Update a versioned document
                companyProfileRepository.save(existingDocument);
            }
            LOGGER.info(String.format("Company %s link deleted in Company Profile",
                    linkRequest.getLinkType()), DataMapHolder.getLogMap());

            companyProfileApiService.invokeChsKafkaApi(linkRequest.getCompanyNumber());
        } catch (IllegalArgumentException exception) {
            LOGGER.error("Error calling chs-kafka-api", exception,
                    DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        } catch (DataAccessException exception) {
            LOGGER.error("Error accessing MongoDB", exception,
                    DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private VersionedCompanyProfileDocument getDocument(String companyNumber) {
        try {
            return companyProfileRepository.findById(companyNumber)
                    .orElseThrow(() -> new DocumentNotFoundException(
                            String.format("Company profile %s not found", companyNumber)));
        } catch (DataAccessException exception) {
            LOGGER.error("Error accessing MongoDB", DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(exception.getMessage());
        }
    }

    private VersionedCompanyProfileDocument createParentCompanyDocument(String parentCompanyNumber) {
        VersionedCompanyProfileDocument parentCompanyDocument = new VersionedCompanyProfileDocument();
        parentCompanyDocument.setId(parentCompanyNumber);
        Data parentCompanyData = new Data();
        Links parentCompanyLinks = new Links();
        String ukEstablishmentLink = String.format("/company/%s/uk-establishments",
                parentCompanyNumber);
        parentCompanyLinks.setUkEstablishments(ukEstablishmentLink);
        parentCompanyData.setLinks(parentCompanyLinks);
        parentCompanyDocument.setCompanyProfile(parentCompanyData);
        return parentCompanyDocument;
    }

    private VersionedCompanyProfileDocument getCompanyProfileDocument(String companyNumber)
            throws ResourceNotFoundException {
        Optional<VersionedCompanyProfileDocument> companyProfileOptional =
                companyProfileRepository.findById(companyNumber);
        return companyProfileOptional.orElseThrow(() ->
                new ResourceNotFoundException(HttpStatus.NOT_FOUND, String.format(
                        RESOURCE_NOT_FOUND_STRING, companyNumber)));
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

    private static void setLinksOnType(Links links, String linkType, String companyNumber) {
        switch (linkType) {
            case "charges" -> links.setCharges(formatLinks(companyNumber, linkType));
            case "exemptions" -> links.setExemptions(formatLinks(companyNumber, linkType));
            case "filing-history" -> links.setFilingHistory(formatLinks(companyNumber, linkType));
            case "insolvency" -> links.setInsolvency(formatLinks(companyNumber, linkType));
            case "officers" -> links.setOfficers(formatLinks(companyNumber, linkType));
            case "persons-with-significant-control" ->
                    links.setPersonsWithSignificantControl(formatLinks(companyNumber, linkType));
            case "persons-with-significant-control-statements" ->
                    links.setPersonsWithSignificantControlStatements(formatLinks(companyNumber, linkType));
            case "uk-establishments" -> links.setUkEstablishments(formatLinks(companyNumber, linkType));
            case "registers" -> links.setRegisters(formatLinks(companyNumber, linkType));
            default -> throw new BadRequestException("DID NOT MATCH KNOWN LINK TYPE");
        }
    }

    private static String formatLinks(String companyNumber, String linkType) {
        return String.format("/company/%s/%s", companyNumber, linkType);
    }

    private static void unsetLinksOnType(Links links, String linkType) {
        switch (linkType) {
            case "charges" -> links.setCharges(null);
            case "exemptions" -> links.setExemptions(null);
            case "filing-history" -> links.setFilingHistory(null);
            case "insolvency" -> links.setInsolvency(null);
            case "officers" -> links.setOfficers(null);
            case "persons-with-significant-control" -> links.setPersonsWithSignificantControl(null);
            case "persons-with-significant-control-statements" -> links.setPersonsWithSignificantControlStatements(null);
            case "uk-establishments" -> links.setUkEstablishments(null);
            case "registers" -> links.setRegisters(null);
            default -> throw new BadRequestException("DID NOT MATCH KNOWN LINK TYPE");
        }
    }

    private void deltaAtCheck(String requestDeltaAt, LocalDateTime existingDeltaAt) {
        if (isDeltaStale(requestDeltaAt, existingDeltaAt)) {
            throw new ConflictException("Stale delta received; request delta_at: [%s] is not after existing delta_at: [%s]".formatted(
                    requestDeltaAt, existingDeltaAt));
        }
    }
}
