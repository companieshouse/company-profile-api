package uk.gov.companieshouse.company.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    Logger logger;

    @InjectMocks
    CompanyProfileService companyProfileService;

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        CompanyProfileApi mockCompanyProfile = new CompanyProfileApi();
        mockCompanyProfile.setCompanyNumber("123456");

        when(companyProfileRepository.findCompanyProfileApiByCompanyNumber(anyString()))
                .thenReturn(Optional.of(mockCompanyProfile));

        Optional<CompanyProfileApi> companyProfile = companyProfileService.get("123456");

        assertThat(companyProfile.get()).isSameAs(mockCompanyProfile);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findCompanyProfileApiByCompanyNumber(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyProfileApi> companyProfile = companyProfileService.get("123456");

        assertTrue(companyProfile.isEmpty());
        verify(logger, times(2)).trace(anyString());
    }
}