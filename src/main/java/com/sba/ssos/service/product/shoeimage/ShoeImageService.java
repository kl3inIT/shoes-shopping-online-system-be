package com.sba.ssos.service.product.shoeimage;

import com.sba.ssos.dto.request.product.shoe.ShoeVariantImageUpdateRequest;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.entity.ShoeImage;
import com.sba.ssos.entity.ShoeVariant;
import com.sba.ssos.repository.ShoeImageRepository;
import com.sba.ssos.service.storage.MinioFileStorageService;
import com.sba.ssos.service.storage.MinioStorageService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ShoeImageService {

  private final MinioFileStorageService storageService;
  private final MinioStorageService minioStorageService;
  private final ShoeImageRepository shoeImageRepository;

  private String toPublicImageUrl(String urlOrObjectKey) {
    if (urlOrObjectKey == null || urlOrObjectKey.isBlank()) {
      return urlOrObjectKey;
    }
    String s = urlOrObjectKey.trim();
    // If already absolute, keep as-is
    if (s.startsWith("http://") || s.startsWith("https://")) {
      return s;
    }
    // Otherwise treat as MinIO objectKey and generate presigned GET URL
    return minioStorageService.getPresignedGetUrl(s);
  }

  public List<String> uploadShoeImages(Shoe shoe, List<ShoeVariant> variants,
      List<MultipartFile> shoeImageFiles) {
    List<String> shoeImageUrls = new ArrayList<>();

    if (shoeImageFiles == null || shoeImageFiles.isEmpty()) {
      return shoeImageUrls;
    }

    for (int sortOrder = 0; sortOrder < shoeImageFiles.size(); sortOrder++) {
      MultipartFile shoeImageFile = shoeImageFiles.get(sortOrder);
      String objectKey = storageService.upload(shoeImageFile, "shoes");

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
        String objectKey = storageService.upload(variantImageFile, "shoevariants");

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

  public void syncShoeImagesForUpdate(
      Shoe shoe,
      List<String> keepShoeImageUrls,
      List<MultipartFile> newShoeImageFiles
  ) {
    if ((keepShoeImageUrls == null || keepShoeImageUrls.isEmpty())
        && (newShoeImageFiles == null || newShoeImageFiles.isEmpty())) {
      return;
    }
    List<ShoeImage> existingImages =
        shoeImageRepository.findByShoe_IdAndShoeVariantIsNullOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            shoe.getId());

    List<String> keepList = keepShoeImageUrls == null ? List.of() : keepShoeImageUrls;
    Set<String> keepSet = new HashSet<>(keepList);

    List<ShoeImage> keptImages = new ArrayList<>();
    for (ShoeImage image : existingImages) {
      if (keepSet.contains(image.getUrl())) {
        keptImages.add(image);
      } else {
        storageService.delete(image.getUrl());
        shoeImageRepository.delete(image);
      }
    }

    Map<String, ShoeImage> keptByUrl = new HashMap<>();
    for (ShoeImage image : keptImages) {
      keptByUrl.putIfAbsent(image.getUrl(), image);
    }

    int nextSortOrder = 0;
    List<ShoeImage> reordered = new ArrayList<>();
    for (String keepUrl : keepList) {
      ShoeImage image = keptByUrl.remove(keepUrl);
      if (image != null) {
        image.setSortOrder(nextSortOrder++);
        reordered.add(image);
      }
    }

    for (ShoeImage image : keptByUrl.values()) {
      image.setSortOrder(nextSortOrder++);
      reordered.add(image);
    }

    if (newShoeImageFiles != null && !newShoeImageFiles.isEmpty()) {
      for (MultipartFile file : newShoeImageFiles) {
        String objectKey = storageService.upload(file, "shoes");
        ShoeImage newImage = ShoeImage.builder()
            .shoe(shoe)
            .shoeVariant(null)
            .url(objectKey)
            .isPrimary(false)
            .sortOrder(nextSortOrder++)
            .build();
        reordered.add(newImage);
      }
    }

    for (int i = 0; i < reordered.size(); i++) {
      reordered.get(i).setPrimary(i == 0);
    }

    shoeImageRepository.saveAll(reordered);
  }

  public void syncVariantImagesForUpdate(
      Shoe shoe,
      List<ShoeVariant> variantsInRequestOrder,
      List<ShoeVariantImageUpdateRequest> variantImageUpdates,
      List<List<MultipartFile>> variantImageFilesList
  ) {
    boolean noKeepInfo = variantImageUpdates == null || variantImageUpdates.isEmpty();
    boolean hasAnyNewVariantFile = variantImageFilesList != null
        && variantImageFilesList.stream()
        .filter(files -> files != null && !files.isEmpty())
        .flatMap(List::stream)
        .anyMatch(file -> file != null && !file.isEmpty());

    if (noKeepInfo && !hasAnyNewVariantFile) {
      return;
    }
    Map<UUID, List<String>> keepUrlsByVariantId = new HashMap<>();
    if (variantImageUpdates != null) {
      for (ShoeVariantImageUpdateRequest update : variantImageUpdates) {
        keepUrlsByVariantId.put(update.variantId(),
            update.keepImageUrls() == null ? List.of() : update.keepImageUrls());
      }
    }

    for (int i = 0; i < variantsInRequestOrder.size(); i++) {
      ShoeVariant variant = variantsInRequestOrder.get(i);
      List<ShoeImage> existingVariantImages =
          shoeImageRepository.findByShoeVariant_IdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
              variant.getId());

      List<String> keepList = keepUrlsByVariantId.getOrDefault(variant.getId(), List.of());
      Set<String> keepSet = new HashSet<>(keepList);

      List<ShoeImage> keptImages = new ArrayList<>();
      for (ShoeImage image : existingVariantImages) {
        if (keepSet.contains(image.getUrl())) {
          keptImages.add(image);
        } else {
          storageService.delete(image.getUrl());
          shoeImageRepository.delete(image);
        }
      }

      Map<String, ShoeImage> keptByUrl = new HashMap<>();
      for (ShoeImage image : keptImages) {
        keptByUrl.putIfAbsent(image.getUrl(), image);
      }

      int nextSortOrder = 0;
      List<ShoeImage> reordered = new ArrayList<>();
      for (String keepUrl : keepList) {
        ShoeImage image = keptByUrl.remove(keepUrl);
        if (image != null) {
          image.setSortOrder(nextSortOrder++);
          reordered.add(image);
        }
      }

      for (ShoeImage image : keptByUrl.values()) {
        image.setSortOrder(nextSortOrder++);
        reordered.add(image);
      }

      List<MultipartFile> newFiles =
          variantImageFilesList != null && i < variantImageFilesList.size()
              ? variantImageFilesList.get(i)
              : List.of();

      for (MultipartFile file : newFiles) {
        String objectKey = storageService.upload(file, "shoevariants");
        ShoeImage newImage = ShoeImage.builder()
            .shoe(shoe)
            .shoeVariant(variant)
            .url(objectKey)
            .isPrimary(false)
            .sortOrder(nextSortOrder++)
            .build();
        reordered.add(newImage);
      }

      for (int j = 0; j < reordered.size(); j++) {
        reordered.get(j).setPrimary(j == 0);
      }

      shoeImageRepository.saveAll(reordered);
    }
  }

  public List<String> getShoeImageUrls(Shoe shoe, List<ShoeVariant> variants) {
    List<String> shoeImageUrls = new ArrayList<>();

    List<ShoeImage> shoeImageEntities =
        shoeImageRepository.findByShoe_IdAndShoeVariantIsNullOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            shoe.getId());

    for (ShoeImage shoeImageEntity : shoeImageEntities) {
      shoeImageUrls.add(toPublicImageUrl(shoeImageEntity.getUrl()));
    }

    return shoeImageUrls.stream().distinct().toList();
  }

  public List<String> getVariantImageUrls(ShoeVariant variant) {
    List<ShoeImage> variantImageEntities =
        shoeImageRepository.findByShoeVariant_IdOrderByIsPrimaryDescSortOrderAscCreatedAtAsc(
            variant.getId());
    return variantImageEntities.stream()
        .map(ShoeImage::getUrl)
        .map(this::toPublicImageUrl)
        .toList();
  }
}
