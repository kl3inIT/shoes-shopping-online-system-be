package com.sba.ssos.controller.category;

import com.sba.ssos.dto.ResponseGeneral;
import com.sba.ssos.dto.request.category.CategoryCreateRequest;
import com.sba.ssos.dto.response.category.CategoryResponse;
import com.sba.ssos.service.category.CategoryService;
import com.sba.ssos.utils.LocaleUtils;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;
  private final LocaleUtils localeUtils;

  @GetMapping
  public ResponseGeneral<List<CategoryResponse>> getAll() {
    var data = categoryService.getAllCategories();
    return ResponseGeneral.ofSuccess(localeUtils.get("success.category.fetched"), data);
  }

  @GetMapping("/{id}")
  public ResponseGeneral<CategoryResponse> getById(@PathVariable UUID id) {
    var data = categoryService.getById(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.category.fetched"), data);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseGeneral<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
    var data = categoryService.create(request);
    return ResponseGeneral.ofCreated(localeUtils.get("success.category.created"), data);
  }

  @PutMapping("/{id}")
  public ResponseGeneral<CategoryResponse> update(
      @PathVariable UUID id, @Valid @RequestBody CategoryCreateRequest request) {
    var data = categoryService.update(id, request);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.category.updated"), data);
  }

  @DeleteMapping("/{id}")
  public ResponseGeneral<Void> delete(@PathVariable UUID id) {
    categoryService.delete(id);
    return ResponseGeneral.ofSuccess(localeUtils.get("success.category.deleted"));
  }
}
