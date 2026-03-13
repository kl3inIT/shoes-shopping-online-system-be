package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserResponse toResponse(User user);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "trimOrNull")
  @Mapping(target = "avatarUrl", source = "avatarUrl", qualifiedByName = "trimOrNull")
  @Mapping(target = "address", source = "address", qualifiedByName = "trimOrNull")
  void updateFromRequest(UpdateUserProfileRequest request, @MappingTarget User user);

  @Named("trimOrNull")
  default String trimOrNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }
}
