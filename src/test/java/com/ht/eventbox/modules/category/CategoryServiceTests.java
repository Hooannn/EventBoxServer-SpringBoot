package com.ht.eventbox.modules.category;

import com.ht.eventbox.entities.Category;
import com.ht.eventbox.modules.event.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTests {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getAllPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Category.builder().id(7L).build()), PageRequest.of(0, 20), 41);
        when(categoryRepository.findAllByOrderByIdAsc(any())).thenReturn(page);

        var result = categoryService.getAll(PageRequest.of(0, 20));

        assertThat(result).isSameAs(page);
        verify(categoryRepository).findAllByOrderByIdAsc(PageRequest.of(0, 20));
    }

    @Test
    void getAllSearchPaged_shouldDelegateToRepository() {
        var page = new PageImpl<>(List.of(Category.builder().id(7L).build()), PageRequest.of(0, 20), 41);
        when(categoryRepository.searchAllByOrderByIdAsc(eq("music"), any())).thenReturn(page);

        var result = categoryService.getAll("music", PageRequest.of(0, 20));

        assertThat(result).isSameAs(page);
        verify(categoryRepository).searchAllByOrderByIdAsc("music", PageRequest.of(0, 20));
    }
}
