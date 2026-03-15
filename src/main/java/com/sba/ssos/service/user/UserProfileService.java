package com.sba.ssos.service.user;

import com.sba.ssos.dto.request.user.UpdateUserProfileRequest;
import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import com.sba.ssos.mapper.UserMapper;
import com.sba.ssos.service.storage.MinioFileStorageService;
import com.sba.ssos.service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final AuthenticatedUserService authenticatedUserService;
  private final UserMapper userMapper;
  private final MinioFileStorageService minioFileStorageService;
  private final MinioStorageService minioStorageService;

  @Transactional(readOnly = true)
  public UserResponse getCurrentUserProfile() {
    return userMapper.toResponse(authenticatedUserService.getCurrentUserEntity(), minioStorageService);
  }

  @Transactional
  public UserResponse updateCurrentUserProfile(UpdateUserProfileRequest request) {
    User user = authenticatedUserService.getCurrentUserEntity();
    userMapper.updateFromRequest(request, user);
    return userMapper.toResponse(user, minioStorageService);
  }

  @Transactional
  public UserResponse uploadAvatar(MultipartFile file) {
    User user = authenticatedUserService.getCurrentUserEntity();
    user.setAvatarUrl(minioFileStorageService.upload(file, "avatars"));
    return userMapper.toResponse(user, minioStorageService);
  }
}
