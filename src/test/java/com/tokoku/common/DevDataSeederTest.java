package com.tokoku.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

import com.tokoku.product.Category;
import com.tokoku.product.CategoryRepository;
import com.tokoku.product.Product;
import com.tokoku.product.ProductRepository;

class DevDataSeederTest {

    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);

    @Test
    void seedsSixCategoriesAndThirtyProductsWhenEmpty() {
        when(products.count()).thenReturn(0L);
        when(categories.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        new DevDataSeeder(categories, products).run(null);

        verify(categories, times(6)).save(any(Category.class));
        verify(products, times(30)).save(any(Product.class));
    }

    @Test
    void doesNothingWhenProductsExist() {
        when(products.count()).thenReturn(1L);

        new DevDataSeeder(categories, products).run(null);

        verify(categories, never()).save(any(Category.class));
        verify(products, never()).save(any(Product.class));
    }
}
