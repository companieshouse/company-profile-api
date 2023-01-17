package uk.gov.companieshouse.company.profile.controller;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.company.CompanyProfile;

import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;
import uk.gov.companieshouse.company.profile.util.LinkRequestFactory;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;
    private final Logger logger;
    private final LinkRequestFactory linkRequestFactory;

    /**
     * Constructor.
     * @param logger logs messages to the console
     * @param companyProfileService Company Profile Service
     * @param linkRequestFactory Link Request Factory
     */
    public CompanyProfileController(Logger logger, CompanyProfileService companyProfileService,
                                    LinkRequestFactory linkRequestFactory) {
        this.logger = logger;
        this.companyProfileService = companyProfileService;
        this.linkRequestFactory = linkRequestFactory;
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
     * Add a company exemptions link to a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/exemptions")
    public ResponseEntity<Void> addExemptionsLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));

        companyProfileService.addExemptionsLink(
                linkRequestFactory.createExemptionsLinkRequest(contextId, companyNumber));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete a company exemptions link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/exemptions/delete")
    public ResponseEntity<Void> deleteExemptionsLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));

        companyProfileService.deleteExemptionsLink(
                linkRequestFactory.createExemptionsLinkRequest(contextId, companyNumber));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Add an officers link to a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/officers")
    public ResponseEntity<Void> addOfficersLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));

        companyProfileService.addOfficersLink(
                linkRequestFactory.createOfficersLinkRequest(contextId, companyNumber));
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    /**
     * Delete a company officers link on a company profile for the given company number.
     *
     * @param companyNumber The number of the company
     * @return no response
     */
    @PatchMapping("/company/{company_number}/links/officers/delete")
    public ResponseEntity<Void> deleteOfficersLink(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Payload successfully received on PATCH endpoint "
                + "with contextId %s and company number %s", contextId, companyNumber));

        companyProfileService.deleteOfficersLink(
                linkRequestFactory.createOfficersLinkRequest(contextId, companyNumber));
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
