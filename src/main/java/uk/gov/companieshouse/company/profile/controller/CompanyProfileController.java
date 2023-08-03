package uk.gov.companieshouse.company.profile.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.exception.ResourceNotFoundException;
import uk.gov.companieshouse.api.exception.ServiceUnavailableException;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;
    private final Logger logger;

    /**
     * Constructor.
     * @param logger logs messages to the console
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
        logger.info(String.format("Request received on GET endpoint for company number %s",
                companyNumber));
        return companyProfileService.get(companyNumber)
                .map(document ->
                        new ResponseEntity<>(
                                new CompanyProfile().data(document.companyProfile),
                                HttpStatus.OK))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * PUT a company profile for a given company number.
     * @param companyProfile   The company profile
     * @param companyNumber The company number of the company
     * @return ResponseEntity
     */
    @PutMapping("/company/{company_number}")
    public ResponseEntity<Void> processCompanyProfile(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile companyProfile) {
        logger.info(String.format("Request received on PUT endpoint for company number %s",
                companyNumber));
        companyProfileService.processCompanyProfile(contextId, companyNumber, companyProfile);
        return ResponseEntity.status(HttpStatus.OK).build();
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
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));
        companyProfileService.updateInsolvencyLink(contextId, companyNumber, requestBody);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * add a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/"
            + "{link_type}")
    public ResponseEntity<Void> addLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));
        companyProfileService.processLinkRequest(linkType, companyNumber, contextId, false);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete a link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/"
            + "{link_type}/delete")
    public ResponseEntity<Void> deleteLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @PathVariable("link_type") String linkType) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));
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
    public ResponseEntity<Data> searchComapnyProfile(@PathVariable("company_number")
                String companyNumber) throws JsonProcessingException, ResourceNotFoundException {
        logger.info(String.format("Received get request for Company Number %s", companyNumber));
        Data data = companyProfileService.retrieveCompanyNumber(companyNumber);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    /**
     * Delete the data object for given company profile number.
     *
     * @param companyNumber The number of the company
     * @return ResponseEntity
     */
    @DeleteMapping("/company/{company_number}")
    public ResponseEntity<Void> deleteCompanyProfile(
            @PathVariable("company_number") String companyNumber) {
        logger.info("Deleting company profile");
        try {
            companyProfileService.deleteCompanyProfile(companyNumber);
            logger.info("Successfully deleted company profile with company number: "
                    + companyNumber);
            return ResponseEntity.status(HttpStatus.OK).build();
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
}
