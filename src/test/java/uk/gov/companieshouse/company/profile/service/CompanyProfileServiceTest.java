package uk.gov.companieshouse.company.profile.service;

import org.junit.Before;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.company.profile.CompanyRepository;
import uk.gov.companieshouse.company.profile.domain.CompanyProfileDao;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyProfileServiceTest {

    private static final String MOCK_COMPANY_NUMBER = "6146287";

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private CompanyProfileService underTest;

    @Mock
    private Logger logger;

    @Before
    void setup() {
        doNothing().when(logger).debug(Mockito.any());
    }

    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() {
        CompanyProfile companyProfile = mockCompanyProfileWithoutInsolvency();
        CompanyProfile companyProfileWithInsolvency = companyProfile;
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");
        when(repository.findByCompanyNumber(Mockito.any())).thenReturn(new CompanyProfileDao(companyProfile));

        underTest.update(companyProfileWithInsolvency);

        verify(repository, Mockito.times(1)).findByCompanyNumber(eq(companyProfile.getData().getCompanyNumber()));
        verify(repository, Mockito.times(1)).save(argThat(companyProfileDao -> {
            assert(companyProfileDao.companyProfile.getData().getLinks().getInsolvency()).equals(companyProfileWithInsolvency.getData().getLinks().getInsolvency());
            return true;
        }));
    }

    private CompanyProfile mockCompanyProfileWithoutInsolvency() {
        CompanyProfile companyProfile = new CompanyProfile();
        Data data = new Data();
        data.setCompanyNumber("12345");

        Links links = new Links();
        links.setOfficers("officer");

        data.setLinks(links);
        companyProfile.setData(data);
        return companyProfile;
    }



    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        CompanyProfileDao mockCompanyProfileDao = new CompanyProfileDao(mockCompanyProfile);

        when(repository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDao));

        Optional<CompanyProfileDao> companyProfileActual =
                underTest.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual.get()).isSameAs(mockCompanyProfileDao);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(repository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyProfileDao> companyProfileActual =
                underTest.get(MOCK_COMPANY_NUMBER);

        assertTrue(companyProfileActual.isEmpty());
        verify(logger, times(2)).trace(anyString());
    }

}
