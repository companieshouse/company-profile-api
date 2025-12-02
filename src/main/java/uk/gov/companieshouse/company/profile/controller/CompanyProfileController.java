package uk.gov.companieshouse.company.profile.controller;

import static uk.gov.companieshouse.company.profile.CompanyProfileApiApplication.APPLICATION_NAME_SPACE;

import com.mongodb.MongoTimeoutException;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.UkEstablishmentsList;
import uk.gov.companieshouse.api.exception.BadRequestException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.api.model.ukestablishments.PrivateUkEstablishmentsAddressListApi;
import uk.gov.companieshouse.company.profile.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class CompanyProfileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final CompanyProfileService companyProfileService;

    /**
     * Constructor.
     *
     * @param companyProfileService Company Profile Service
     */
    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    /**
     * Get the data object for given company profile number.
     *
     * @param companyNumber The number of the company
     * @return data object
     */
    @GetMapping("/company/{company_number}")
    public ResponseEntity<Data> searchCompanyProfile(
            @PathVariable("company_number") String companyNumber)
            throws ResourceNotFoundException {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing GET company profile", DataMapHolder.getLogMap());
        try {
            Data data = companyProfileService.retrieveCompanyNumber(companyNumber);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (DataAccessException dataAccessException) {
            LOGGER.error("Error while trying to retrieve company profile", dataAccessException,
                    DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * Retrieve a company profile for a given company number.
     *
     * @param companyNumber The company number of the company
     * @return The company profile
     */
    @GetMapping("/company/{company_number}/links")
    public ResponseEntity<CompanyProfile> getCompanyProfile(
            @PathVariable("company_number") String companyNumber) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing GET company links", DataMapHolder.getLogMap());

        VersionedCompanyProfileDocument document = companyProfileService.get(companyNumber);

        return new ResponseEntity<>(new CompanyProfile().data(document.companyProfile), HttpStatus.OK);
    }

    /**
     * Get the company details object for given company number.
     *
     * @param companyNumber The number of the company
     * @return company details object
     */
    @GetMapping("/company/{company_number}/company-detail")
    public ResponseEntity<CompanyDetails> getCompanyDetails(
            @PathVariable("company_number") String companyNumber)
            throws ResourceNotFoundException {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing GET company profile detail",
                DataMapHolder.getLogMap());
        try {
            CompanyDetails companyDetails = companyProfileService.getCompanyDetails(companyNumber);
            return new ResponseEntity<>(companyDetails, HttpStatus.OK);
        } catch (DataAccessException dataAccessException) {
            LOGGER.error("Error while trying to get company details.", dataAccessException, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * Retrieve a list of uk establishments for a given parent company number.
     *
     * @param parentCompanyNumber the supplied parent company number
     * @return list of uk establishments
     */
    @GetMapping("/company/{company_number}/uk-establishments")
    public ResponseEntity<UkEstablishmentsList> getUkEstablishments(
            @PathVariable("company_number") String parentCompanyNumber) {
        DataMapHolder.get().companyNumber(parentCompanyNumber);
        LOGGER.info(
                "Processing GET company profile uk establishments",
                DataMapHolder.getLogMap());
        try {
            UkEstablishmentsList data = companyProfileService
                    .getUkEstablishments(parentCompanyNumber);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (DataAccessException dataAccessException) {
            LOGGER.error("Error accessing MongoDB for company.", dataAccessException, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * Retrieve a list of uk establishments addresses for a given parent company number.
     *
     * @param parentCompanyNumber the supplied parent company number
     * @return list of uk establishments addresses
     */
    @GetMapping("/company/{company_number}/uk-establishments/addresses")
    public ResponseEntity<PrivateUkEstablishmentsAddressListApi> getUkEstablishmentsAddresses(
            @PathVariable("company_number") String parentCompanyNumber) {
        DataMapHolder.get().companyNumber(parentCompanyNumber);
        LOGGER.info(
                "Processing GET company profile uk establishments addresses",
                DataMapHolder.getLogMap());
        try {
            PrivateUkEstablishmentsAddressListApi data = companyProfileService
                    .getUkEstablishmentsAddresses(parentCompanyNumber);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (DataAccessException dataAccessException) {
            LOGGER.error("Error accessing MongoDB for company.", dataAccessException, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * PUT a company profile for a given company number.
     *
     * @param companyProfile The company profile
     * @param companyNumber  The company number of the company
     * @return ResponseEntity
     */
    @PutMapping("/company/{company_number}/internal")
    public ResponseEntity<Void> processCompanyProfile(
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile companyProfile) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing company profile upsert", DataMapHolder.getLogMap());
        try {
            companyProfileService.processCompanyProfile(companyNumber, companyProfile);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (ServiceUnavailableException | MongoTimeoutException ex) {
            LOGGER.error(ex, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (BadRequestException ex) {
            LOGGER.error(ex, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * Update a company insolvency link.
     *
     * @param companyNumber The company number of the company
     * @param requestBody   The company profile
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links")
    public ResponseEntity<Void> updateCompanyProfile(
            @PathVariable("company_number") String companyNumber,
            @Valid @RequestBody CompanyProfile requestBody) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing company links PATCH", DataMapHolder.getLogMap());
        companyProfileService.updateInsolvencyLink(companyNumber, requestBody);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Add a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @param linkType      The type of link
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/{link_type}")
    public ResponseEntity<Void> addLink(
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing company link type PATCH", DataMapHolder.getLogMap());
        companyProfileService.processLinkRequest(linkType, companyNumber, false);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @param linkType      The type of link
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/{link_type}/delete")
    public ResponseEntity<Void> deleteLink(
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing company link type delete PATCH",
                DataMapHolder.getLogMap());
        companyProfileService.processLinkRequest(linkType, companyNumber, true);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete the data object for given company profile number.
     *
     * @param companyNumber The number of the company
     * @return ResponseEntity
     */
    @DeleteMapping("/company/{company_number}/internal")
    public ResponseEntity<Void> deleteCompanyProfile(
            @RequestHeader("X-DELTA-AT") String deltaAt,
            @PathVariable("company_number") String companyNumber) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        LOGGER.info("Processing DELETE company profile", DataMapHolder.getLogMap());
        try {
            companyProfileService.deleteCompanyProfile(companyNumber, deltaAt);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (DataAccessException | MongoTimeoutException ex) {
            LOGGER.error("Error while trying to delete company profile.", ex, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

}
