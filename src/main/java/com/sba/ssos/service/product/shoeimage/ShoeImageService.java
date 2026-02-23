package com.sba.ssos.service.product.shoeimage;

import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeImage;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.repository.ShoeImageRepository;
import com.sba.ssos.service.storage.MinioFileStorageService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ShoeImageService {

  private final MinioFileStorageService storageService;
  private final ShoeImageRepository shoeImageRepository;

  public List<String> uploadShoeImages(Shoe shoe, List<ShoeVariant> variants,
      List<MultipartFile> shoeImageFiles) {
    List<String> shoeImageUrls = new ArrayList<>();

    if (shoeImageFiles == null || shoeImageFiles.isEmpty()) {
      return shoeImageUrls;
    }

    for (int sortOrder = 0; sortOrder < shoeImageFiles.size(); sortOrder++) {
      MultipartFile shoeImageFile = shoeImageFiles.get(sortOrder);
      String objectKey = storageService.upload(shoeImageFile);

      ShoeImage shoeImage = ShoeImage.builder()
          .shoe(shoe)
          .shoeVariant(null) // Ảnh chung của shoe, không gắn với variant cụ thể
          .url(objectKey)
          .isPrimary(sortOrder == 0) // mặc định ảnh đầu tiên là ảnh chính
          .sortOrder(sortOrder)
          .build();

      shoeImageRepository.save(shoeImage);
      shoeImageUrls.add(objectKey);
    }

    return shoeImageUrls;
  }

  public void uploadVariantImages(Shoe shoe, List<ShoeVariant> variants,
      List<List<MultipartFile>> variantImageFilesList) {

    if (variantImageFilesList == null || variantImageFilesList.isEmpty() || variants.isEmpty()) {
      return;
    }

    for (int i = 0; i < variantImageFilesList.size() && i < variants.size(); i++) {
      List<MultipartFile> variantImageFiles = variantImageFilesList.get(i);

      if (variantImageFiles == null || variantImageFiles.isEmpty()) {
        continue;
      }

      ShoeVariant variant = variants.get(i);

      for (int sortOrder = 0; sortOrder < variantImageFiles.size(); sortOrder++) {
        MultipartFile variantImageFile = variantImageFiles.get(sortOrder);
        String objectKey = storageService.upload(variantImageFile);

        ShoeImage shoeImage = ShoeImage.builder()
            .shoe(shoe)
            .shoeVariant(variant)
            .url(objectKey)
            .isPrimary(sortOrder == 0) // mặc định ảnh đầu tiên của variant là ảnh chính
            .sortOrder(sortOrder)
            .build();

        shoeImageRepository.save(shoeImage);
      }
    }
  }

  public List<String> getShoeImageUrls(Shoe shoe, List<ShoeVariant> variants) {
    List<String> shoeImageUrls = new ArrayList<>();

    // Lấy ảnh chung của shoe (shoeVariant = null)
    List<ShoeImage> shoeImageEntities =
        shoeImageRepository.findByShoe_IdAndShoeVariantIsNullOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            shoe.getId());

    for (ShoeImage shoeImageEntity : shoeImageEntities) {
      shoeImageUrls.add(shoeImageEntity.getUrl());
    }

    return shoeImageUrls.stream().distinct().toList();
  }

  public List<String> getVariantImageUrls(ShoeVariant variant) {
    List<ShoeImage> variantImageEntities =
        shoeImageRepository.findByShoeVariant_IdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            variant.getId());
    return variantImageEntities.stream()
        .map(ShoeImage::getUrl)
        .toList();
  }
}

