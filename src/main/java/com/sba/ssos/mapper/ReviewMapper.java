package com.sba.ssos.mapper;

import com.sba.ssos.dto.request.review.ReviewRequest;
import com.sba.ssos.dto.response.review.ReviewResponse;
import com.sba.ssos.entity.Review;
import com.sba.ssos.entity.ReviewImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ReviewMapper {
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "shoeVariant", ignore = true)
    @Mapping(target = "status", ignore = true)
    Review toEntity(ReviewRequest request);


    @Mapping(source = "review.id", target = "id")
    @Mapping(source = "review.customer.id", target = "customerId")
    @Mapping(target = "customerName", expression = "java(review.getCustomer().getUser().getFirstName() + \" \" + review.getCustomer().getUser().getLastName())")
    @Mapping(source = "review.shoeVariant.id", target = "shoeVariantId")
    @Mapping(source = "review.shoeVariant.shoe.name", target = "shoeName")
    @Mapping(target = "variantInfo", expression = "java(review.getShoeVariant().getSize() + \" - \" + review.getShoeVariant().getColor())")
    @Mapping(source = "review.numberStars", target = "numberStars")
    @Mapping(source = "review.description", target = "description")
    @Mapping(source = "review.status", target = "status")
    @Mapping(target = "imageUrls", expression = "java(mapImages(reviewImages))")
    @Mapping(source = "review.createdAt", target = "createdAt")
    @Mapping(source = "review.lastUpdatedAt", target = "updatedAt")
    ReviewResponse toResponse(Review review, List<ReviewImage> reviewImages);

    default List<String> mapImages(List<ReviewImage> images) {
        if (images == null) return List.of();
        return images.stream().map(ReviewImage::getUrl).collect(Collectors.toList());
    }
}
