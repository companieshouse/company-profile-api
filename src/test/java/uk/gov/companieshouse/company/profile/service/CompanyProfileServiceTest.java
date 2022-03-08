package uk.gov.companieshouse.company.profile.service;

import org.junit.Before;
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

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CompanyProfileServiceTest {

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

        Mockito.verify(repository, Mockito.times(1)).findByCompanyNumber(eq(companyProfile.getData().getCompanyNumber()));
        Mockito.verify(repository, Mockito.times(1)).save(argThat(companyProfileDao -> {
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
