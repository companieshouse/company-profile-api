package uk.gov.companieshouse.company.profile.service;

import com.google.gson.Gson;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.api.company.Links;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.company.profile.api.CompanyProfileApiService;
import uk.gov.companieshouse.company.profile.model.CompanyProfileDocument;
import uk.gov.companieshouse.company.profile.repository.CompanyProfileRepository;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {
    private static final String MOCK_COMPANY_NUMBER = "6146287";
    private static final String MOCK_CONTEXT_ID = "123456";
    private static final String exemptionsLinkType = "exemptions";
    private static final String officersLinkType = "officers";
    private static final String pscStatementsLinkType = "persons-with-significant-control-statements";

    @Mock
    CompanyProfileRepository companyProfileRepository;

    @Mock
    MongoTemplate mongoTemplate;

    @Mock
    Logger logger;

    @Mock
    ApiResponse<Void> apiResponse;

    @Mock
    CompanyProfileApiService companyProfileApiService;

    @Mock
    private CompanyProfileDocument document;

    @Mock
    private Data data;

    @Mock
    private Links links;

    @Mock
    private UpdateResult updateResult;

    @InjectMocks
    CompanyProfileService companyProfileService;

    private Gson gson = new Gson();


}

