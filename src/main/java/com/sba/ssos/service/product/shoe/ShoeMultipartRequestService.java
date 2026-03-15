package com.sba.ssos.service.product.shoe;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@Service
public class ShoeMultipartRequestService {

  public List<List<MultipartFile>> extractVariantImages(
      int variantCount, MultipartHttpServletRequest multipartRequest) {
    List<List<MultipartFile>> variantImages = new ArrayList<>();

    for (int index = 0; index < variantCount; index++) {
      List<MultipartFile> files = multipartRequest.getFiles("variantImages" + index);
      variantImages.add(files != null ? files : List.of());
    }

    return variantImages;
  }
}
