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
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    Logger logger;

    @InjectMocks
    CompanyProfileService companyProfileService;

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        CompanyProfileDao mockCompanyProfileDao = new CompanyProfileDao(mockCompanyProfile);

        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDao));

        Optional<CompanyProfileDao> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual.get()).isSameAs(mockCompanyProfileDao);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyProfileDao> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertTrue(companyProfileActual.isEmpty());
        verify(logger, times(2)).trace(anyString());
    }
}