package com.sba.ssos.service.product.shoe;

import com.sba.ssos.dto.request.product.shoe.ShoeCreateRequest;
import com.sba.ssos.dto.response.product.shoe.ShoeResponse;
import com.sba.ssos.entity.Brand;
import com.sba.ssos.entity.Category;
import com.sba.ssos.entity.Shoe;
import com.sba.ssos.exception.base.NotFoundException;
import com.sba.ssos.mapper.ShoeMapper;
import com.sba.ssos.repository.ShoeRepository;
import com.sba.ssos.utils.SlugUtils;
import com.sba.ssos.service.brand.BrandService;
import com.sba.ssos.service.category.CategoryService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShoeService {

    private final ShoeRepository shoeRepository;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ShoeMapper shoeMapper;

    @Transactional
    public ShoeResponse create(ShoeCreateRequest request){
        Category category = categoryService.findById(request.categoryId());
        Brand brand = brandService.findById(request.brandId());

        String name = request.name().trim();
        String description = request.description().trim();
        String material = request.material().trim();
        String slug = generateUniqueSlug(SlugUtils.slugify(name), null);

        Shoe shoe = new Shoe();
        shoe.setName(name);
        shoe.setSlug(slug);
        shoe.setGender(request.gender());
        shoe.setStatus(request.status());
        shoe.setDescription(description);
        shoe.setMaterial(material);
        shoe.setPrice(request.price());
        shoe.setCategory(category);
        shoe.setBrand(brand);

        shoeRepository.save(shoe);

        return new ShoeResponse(
                shoe.getId(),
                shoe.getName(),
                shoe.getDescription(),
                shoe.getSlug(),
                shoe.getMaterial(),
                shoe.getGender(),
                shoe.getStatus(),
                shoe.getCategory().getId(),
                shoe.getCategory().getName(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getPrice(),
                List.of(),
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt()
        );
    }

    public List<ShoeResponse> getAll() {
        var shoes = shoeRepository.findAll();

        return shoes.stream()
                .map(shoe -> new ShoeResponse(
                        shoe.getId(),
                        shoe.getName(),
                        shoe.getDescription(),
                        shoe.getSlug(),
                        shoe.getMaterial(),
                        shoe.getGender(),
                        shoe.getStatus(),
                        shoe.getCategory().getId(),
                        shoe.getCategory().getName(),
                        shoe.getBrand().getId(),
                        shoe.getBrand().getName(),
                        shoe.getPrice(),
                        List.of(),                // chưa có variants
                        shoe.getCreatedAt(),
                        shoe.getLastUpdatedAt()
                ))
                .toList();
    }

    public ShoeResponse getById(UUID id) {
        Shoe shoe = shoeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shoe", id));

        return new ShoeResponse(
                shoe.getId(),
                shoe.getName(),
                shoe.getDescription(),
                shoe.getSlug(),
                shoe.getMaterial(),
                shoe.getGender(),
                shoe.getStatus(),
                shoe.getCategory().getId(),
                shoe.getCategory().getName(),
                shoe.getBrand().getId(),
                shoe.getBrand().getName(),
                shoe.getPrice(),
                List.of(),               // hiện tại chưa map variants
                shoe.getCreatedAt(),
                shoe.getLastUpdatedAt()
        );
    }


  private String generateUniqueSlug(String baseSlug, UUID excludeId) {
    if (baseSlug == null || baseSlug.isBlank()) {
      return "";
    }

    String candidate = baseSlug;
    int suffix = 0;

    while (true) {
      Optional<Shoe> existing = shoeRepository.findBySlug(candidate);
      if (existing.isEmpty()) {
        return candidate;
      }
      if (excludeId != null && existing.get().getId().equals(excludeId)) {
        return candidate;
      }
      suffix++;
      candidate = baseSlug + "-" + suffix;
    }
  }

}
