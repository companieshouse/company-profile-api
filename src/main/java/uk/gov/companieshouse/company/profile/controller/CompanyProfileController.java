package uk.gov.companieshouse.company.profile.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    /**
     * Retrieve a company profile using a company number.
     *
     * @param companyNumber the company number of the company
     * @return company profile api
     */
    @GetMapping("/company/{company_number}")
    public ResponseEntity<CompanyProfile> getCompanyProfile(
            @PathVariable("company_number") String companyNumber) {
        try {
            return companyProfileService.get(companyNumber)
                    .map(companyProfileDao ->
                            new ResponseEntity<>(companyProfileDao.companyProfile, HttpStatus.OK))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception exception) {
            // TODO Exception handler - code 401
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
