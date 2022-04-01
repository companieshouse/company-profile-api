package uk.gov.companieshouse.company.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.NoSuchElementException;
import java.util.Optional;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.gov.companieshouse.GenerateEtagUtil;
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
    private static final String MOCK_CONTEXT_ID = "123456";

    private static String COMPANY_PROFILE_COLLECTION = "company_profile";

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    Logger logger;

    @Mock
    InsolvencyApiService insolvencyApiService;

    @InjectMocks
    CompanyProfileService companyProfileService;

    private Gson gson;

    @Test
    @DisplayName("When company profile is retrieved successfully then it is returned")
    void getCompanyProfile() {
        Data companyData = new Data().companyNumber(MOCK_COMPANY_NUMBER);
        CompanyProfileDocument mockCompanyProfileDocument = new CompanyProfileDocument(companyData);
        mockCompanyProfileDocument.setId(MOCK_COMPANY_NUMBER);

        when(companyProfileRepository.findById(anyString()))
                .thenReturn(Optional.of(mockCompanyProfileDocument));

        Optional<CompanyProfileDocument> companyProfileActual =
                companyProfileService.get(MOCK_COMPANY_NUMBER);

        assertThat(companyProfileActual.get()).isSameAs(mockCompanyProfileDocument);
        verify(logger, times(2)).trace(anyString());
    }

    @Test
    @DisplayName("When no company profile is retrieved then return empty optional")
    void getNoCompanyProfileReturned() {
        when(companyProfileRepository.findById(anyString()))
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
        when(companyProfileRepository.save(any())).thenReturn(null);
        doReturn(UpdateResult.acknowledged(0l, 0l, null)).when(mongoTemplate).updateFirst(any(), any(), eq(COMPANY_PROFILE_COLLECTION));

        companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, companyProfileWithInsolvency);


        verify(mongoTemplate, times(1)).updateFirst(argThat(findQuery -> {
                    assert(findQuery.getQueryObject().toJson()).equals(expectedFindQuery(companyProfileWithInsolvency.getData().getCompanyNumber()));
                    return true;
                }
        ), argThat(updateQuery -> {
            assert(updateQuery.getUpdateObject().toJson()).contains(expectedUpdateQuery(companyProfileWithInsolvency.getData().getLinks().getInsolvency()));
            return true;
        }), eq(COMPANY_PROFILE_COLLECTION));
//        verify(companyProfileRepository).save(argThat(companyProfileDocument -> {
//            System.out.println(gson.toJson(companyProfileDocument));
//            System.out.println(gson.toJson(generateCompanyProfileDocument(companyProfileWithInsolvency)));
//           assertThat(gson.toJson(companyProfileDocument)).isEqualTo(gson.toJson(generateCompanyProfileDocument(companyProfileWithInsolvency)));
//           return true;
//        }));
    }


    @Test
    void when_insolvency_data_is_given_then_data_should_be_saved() throws Exception {
        CompanyProfile companyProfile = mockCompanyProfileWithoutInsolvency();
        CompanyProfile companyProfileWithInsolvency = companyProfile;
        companyProfileWithInsolvency.getData().getLinks().setInsolvency("INSOLVENCY_LINK");
        doReturn(UpdateResult.acknowledged(1l, 1l, null)).when(mongoTemplate).updateFirst(any(), any(), eq(COMPANY_PROFILE_COLLECTION));

        companyProfileService.updateInsolvencyLink(MOCK_CONTEXT_ID, MOCK_COMPANY_NUMBER, companyProfileWithInsolvency);

        verify(mongoTemplate, times(1)).updateFirst(argThat(findQuery -> {
                    assert(findQuery.getQueryObject().toJson()).equals(expectedFindQuery(companyProfileWithInsolvency.getData().getCompanyNumber()));
                    return true;
                }
        ), argThat(updateQuery -> {
            System.out.println(updateQuery.getUpdateObject().toJson());
            assert(updateQuery.getUpdateObject().toJson()).contains(expectedUpdateQuery(companyProfileWithInsolvency.getData().getLinks().getInsolvency()));
            return true;
        }), eq(COMPANY_PROFILE_COLLECTION));
    }

    private CompanyProfile mockCompanyProfileWithoutInsolvency() {
        CompanyProfile companyProfile = new CompanyProfile();
        Data data = new Data();
        data.setCompanyNumber(MOCK_COMPANY_NUMBER);

        Links links = new Links();
        links.setOfficers("officer");

        data.setLinks(links);
        companyProfile.setData(data);
        return companyProfile;
    }

    private CompanyProfileDocument generateCompanyProfileDocument(CompanyProfile companyProfile) {
        CompanyProfileDocument companyProfileDocument = new CompanyProfileDocument(companyProfile.getData());
        companyProfileDocument.setId(companyProfile.getData().getCompanyNumber());
        return companyProfileDocument;
    }

    private String expectedFindQuery(String companyNumber) {
        return String.format("{\"data.company_number\": \"%s\"}", companyNumber);
    }

    private String expectedUpdateQuery(String insolvencyLink) {
        return String.format("{\"$set\": {\"data.links.insolvency\": \"%s\", \"data.etag\": \"", insolvencyLink);
    }
}