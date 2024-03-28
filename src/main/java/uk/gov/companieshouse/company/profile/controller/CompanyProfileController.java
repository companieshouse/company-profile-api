package uk.gov.companieshouse.company.profile.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Optional;
import javax.validation.Valid;
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
import org.springframework.web.client.HttpClientErrorException;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.ResourceNotFoundException;
import uk.gov.companieshouse.company.profile.logging.DataMapHolder;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;
    private final Logger logger;

    /**
     * Constructor.
     *
     * @param logger                logs messages to the console
     * @param companyProfileService Company Profile Service
     */
    public CompanyProfileController(Logger logger, CompanyProfileService companyProfileService) {
        this.logger = logger;
        this.companyProfileService = companyProfileService;
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
        logger.info(String.format("Request received on GET endpoint for company number %s",
                companyNumber), DataMapHolder.getLogMap());
        try {
            return companyProfileService.get(companyNumber)
                    .map(document ->
                            new ResponseEntity<>(
                                    new CompanyProfile().data(document.companyProfile),
                                    HttpStatus.OK))
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (HttpClientErrorException.Forbidden forbidden) {
            logger.info("Forbidden request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * PUT a company profile for a given company number.
     *
     * @param companyProfile The company profile
     * @param companyNumber  The company number of the company
     * @return ResponseEntity
     */
    @PutMapping("/company/{company_number}")
    public ResponseEntity<Void> processCompanyProfile(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile companyProfile) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.infoContext(contextId, String.format("Request received on PUT endpoint "
                + "for company number %s", companyNumber), DataMapHolder.getLogMap());
        try {
            companyProfileService.processCompanyProfile(contextId, companyNumber, companyProfile);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (HttpClientErrorException.Forbidden forbidden) {
            logger.info("Forbidden request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (DataAccessException dataAccessException) {
            logger.error("Error while trying to update company details: "
                    + dataAccessException.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
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
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @Valid @RequestBody CompanyProfile requestBody) throws ApiErrorResponseException {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.infoContext(contextId, String.format("Payload received on insolvency PATCH endpoint "
                + "for company number %s", companyNumber), DataMapHolder.getLogMap());
        companyProfileService.updateInsolvencyLink(contextId, companyNumber, requestBody);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Add a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @param linkType The type of link
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/{link_type}")
    public ResponseEntity<Void> addLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.infoContext(contextId, String.format("Payload received for the PATCH links endpoint "
                + "with company number %s", companyNumber), DataMapHolder.getLogMap());
        companyProfileService.processLinkRequest(linkType, companyNumber, contextId, false);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @param linkType The type of link
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/{link_type}/delete")
    public ResponseEntity<Void> deleteLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.infoContext(contextId, String.format("Payload received on the DELETE links endpoint "
                + "with company number %s", companyNumber), DataMapHolder.getLogMap());
        companyProfileService.processLinkRequest(linkType, companyNumber, contextId, true);
        return ResponseEntity.status(HttpStatus.OK).build();
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
            throws JsonProcessingException, ResourceNotFoundException {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.info(String.format("Received get request for Company Number %s", companyNumber),
                DataMapHolder.getLogMap());
        try {
            Data data = companyProfileService.retrieveCompanyNumber(companyNumber);
            return new ResponseEntity<>(data, HttpStatus.OK);
        } catch (HttpClientErrorException.Forbidden forbidden) {
            logger.info("Forbidden request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            logger.error("Error while trying to delete company profile: "
                    + resourceNotFoundException.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DataAccessException dataAccessException) {
            logger.error("Error while trying to delete company profile: "
                    + dataAccessException.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * Delete the data object for given company profile number.
     *
     * @param companyNumber The number of the company
     * @return ResponseEntity
     */
    @DeleteMapping("/company/{company_number}")
    public ResponseEntity<Void> deleteCompanyProfile(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.info(String.format("Deleting company profile with company number %s", companyNumber),
                DataMapHolder.getLogMap());
        try {
            companyProfileService.deleteCompanyProfile(contextId, companyNumber);
            logger.info("Successfully deleted company profile with company number: "
                    + companyNumber, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (ResourceNotFoundException resourceNotFoundException) {
            logger.error("Error while trying to delete company profile.",
                    resourceNotFoundException, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (DataAccessException dataAccessException) {
            logger.error("Error while trying to delete company profile.",
                    dataAccessException, DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (HttpClientErrorException.Forbidden forbidden) {
            logger.info("Forbidden request");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
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
            throws JsonProcessingException, ResourceNotFoundException {
        DataMapHolder.get()
                .companyNumber(companyNumber);
        logger.info(String.format("Received get request for company details"
                + " for Company Number %s", companyNumber), DataMapHolder.getLogMap());
        try {
            Optional<CompanyDetails> companyDetails = companyProfileService
                    .getCompanyDetails(companyNumber);
            return companyDetails.map(details -> ResponseEntity.ok().body(details))
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());

        } catch (DataAccessException dataAccessException) {
            logger.error("Error while trying to get company details.", dataAccessException,
                    DataMapHolder.getLogMap());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

}
