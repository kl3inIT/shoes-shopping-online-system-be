package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.service.storage.MinioStorageService;
import org.mapstruct.BeanMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "avatarUrl", source = "avatarUrl", qualifiedByName = "avatarKeyToUrl")
  UserResponse toResponse(User user, @Context MinioStorageService minioStorageService);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "trimOrNull")
  @Mapping(target = "avatarUrl", source = "avatarUrl", qualifiedByName = "trimOrNull")
  @Mapping(target = "address", source = "address", qualifiedByName = "trimOrNull")
  void updateFromRequest(UpdateUserProfileRequest request, @MappingTarget User user);

  @Named("trimOrNull")
  default String trimOrNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }

  @Named("avatarKeyToUrl")
  default String avatarKeyToUrl(String avatarKey, @Context MinioStorageService minioStorageService) {
    return (avatarKey == null || avatarKey.isBlank()) ? null
        : minioStorageService.getPresignedGetUrl(avatarKey);
  }
}
