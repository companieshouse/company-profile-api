package uk.gov.companieshouse.company.profile.controller;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;

@RestController
public class CompanyProfileController {

    private final CompanyProfileRepository companyProfileRepository;

    public CompanyProfileController(CompanyProfileRepository companyProfileRepository) {
        this.companyProfileRepository = companyProfileRepository;
    }

    // TODO Update with newly generated CompanyProfile object once DSND-524 is completed
    /**
     * Retrieve a company profile using a company number.
     * @param companyNumber the company number of the company
     * @return company profile api
     */
    @GetMapping("/company/{company_number}")
    public ResponseEntity<CompanyProfileApi> getCompanyProfile(
            @PathVariable("company_number") String companyNumber) {
        try {
            Optional<CompanyProfileApi> companyProfile =
                    companyProfileRepository.findCompanyProfileApiByCompanyNumber(companyNumber);

            if (companyProfile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            return ResponseEntity.status(HttpStatus.OK).body(companyProfile.get());
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
