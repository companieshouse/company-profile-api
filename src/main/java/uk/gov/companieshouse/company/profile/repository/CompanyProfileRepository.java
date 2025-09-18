package uk.gov.companieshouse.company.profile.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.company.profile.model.VersionedCompanyProfileDocument;

@Repository
public interface CompanyProfileRepository extends MongoRepository<VersionedCompanyProfileDocument, String> {

    @Query("{'parent_company_number' : '?0'}")
    List<VersionedCompanyProfileDocument> findAllByParentCompanyNumber(String parentCompanyNumber);

    @Query(value = "{'parent_company_number' : '?0', 'data.company_status': 'open'}", sort = "{'data.date_of_creation': -1}")
    List<VersionedCompanyProfileDocument> findAllOpenCompanyProfilesByParentNumberSortedByCreation(String parentCompanyNumber);
}
