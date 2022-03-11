package uk.gov.companieshouse.company.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.company.CompanyProfile;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.company.profile.api.InsolvencyApiService;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    Logger logger;

    @Mock
    InsolvencyApiService insolvencyApiService;

    @InjectMocks
    CompanyProfileService companyProfileService;

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        CompanyProfile mockCompanyProfile = new CompanyProfile();
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        mockCompanyProfile.setData(companyData);
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(mockCompanyProfile);

        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        Optional<CompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual.get()).isSameAs(mockCompanyProfileDocument);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(anyString()))
                .thenReturn(Optional.empty());

        Optional<CompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertTrue(companyProfileActual.isEmpty());
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When insolvency is given but company doesnt exist with that company number, NoSuchElementException exception thrown")
    void when_insolvency_data_is_given_then_data_should_be_saved_not_found() {
        CompanyProfile companyProfile = mockCompanyProfileWithoutInsolvency();
        CompanyProfile companyProfileWithInsolvency = companyProfile;
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");
        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(any())).thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> companyProfileService.updateInsolvencyLink(companyProfileWithInsolvency),
                "Expected doThing() to throw, but it didn't"
        );
        verify(companyProfileRepository, times(1)).findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(eq(companyProfile.getData().getCompanyNumber()));
    }


    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() throws Exception {
        CompanyProfile companyProfile = mockCompanyProfileWithoutInsolvency();
        CompanyProfile companyProfileWithInsolvency = companyProfile;
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");
        when(companyProfileRepository.findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(any())).thenReturn(Optional.of(new CompanyProfileDocument(companyProfile)));

        companyProfileService.updateInsolvencyLink(companyProfileWithInsolvency);

        verify(companyProfileRepository, times(1)).findCompanyProfileDaoByCompanyProfile_Data_CompanyNumber(eq(companyProfile.getData().getCompanyNumber()));
        verify(companyProfileRepository, times(1)).save(argThat(companyProfileDao -> {
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
}