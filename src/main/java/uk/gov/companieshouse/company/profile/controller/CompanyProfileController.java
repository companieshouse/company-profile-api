package uk.gov.companieshouse.company.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@RestController
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @PatchMapping("/company/{company_number}/links")
    public ResponseEntity<String> updateCompanyProfile(
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile requestBody
    ) {
        companyProfileService.update(requestBody);
        return ResponseEntity.status(HttpStatus.OK).body("OK");
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
        return companyProfileService.get(companyNumber)
                .map(companyProfileDao ->
                        new ResponseEntity<>(companyProfileDao.companyProfile, HttpStatus.OK))
                .orElse(ResponseEntity.notFound().build());
    }
}
