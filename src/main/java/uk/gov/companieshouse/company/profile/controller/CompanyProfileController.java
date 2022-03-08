package uk.gov.companieshouse.company.profile.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.company.profile.service.CompanyProfileService;

@RestController
public class CompanyProfileController {

    @Autowired
    private CompanyProfileService companyProfileService;

    @PatchMapping("/company/{company_number}/links")
    public ResponseEntity<String> updateCompanyProfile(
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile requestBody
    ) {
        companyProfileService.update(requestBody);
        return ResponseEntity.status(HttpStatus.OK).body("OK");
    }
}
