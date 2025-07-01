package com.ht.eventbox.modules.category.dtos;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBulkCategoriesDto {

    List<CreateCategoryDto> categories;
}
