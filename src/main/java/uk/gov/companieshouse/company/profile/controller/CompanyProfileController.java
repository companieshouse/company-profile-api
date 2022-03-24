package uk.gov.companieshouse.company.profile.controller;

import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
        return companyProfileService.get(companyNumber)
                .map(companyProfileDao ->
                        new ResponseEntity<>(
                                new CompanyProfile().data(companyProfileDao.companyProfile),
                                HttpStatus.OK))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a company insolvency link.
     *
     * @param companyNumber the company number of the company
     * @param requestBody The company profile
     */
    @PatchMapping("/company/{company_number}/links")
    public ResponseEntity<Void> updateCompanyProfile(
            @PathVariable("company_number") String companyNumber,
            @RequestBody CompanyProfile requestBody
    ) {
        try {
            companyProfileService.updateInsolvencyLink(requestBody);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (NoSuchElementException noSuchElementException) {
            return ResponseEntity.notFound().build();
        }

    }
}