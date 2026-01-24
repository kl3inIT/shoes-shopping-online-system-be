package com.sba.ssos.mapper;

import com.sba.ssos.dto.response.user.UserResponse;
import com.sba.ssos.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserResponse toResponse(User user);
}
