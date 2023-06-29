package uk.gov.companieshouse.company.profile.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import uk.gov.companieshouse.api.company.Links;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface LinksMapper {
    void mapLinks(@MappingTarget Links existingLink, Links deltaLink);
}
