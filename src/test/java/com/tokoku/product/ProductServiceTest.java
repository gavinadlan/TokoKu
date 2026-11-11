package com.tokoku.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tokoku.common.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock ProductImageUrls urls;
    @InjectMocks ProductService service;

    private final Category category = new Category("Gadget");

    @Test
    void createSavesProduct() {
        when(categories.findById(1L)).thenReturn(Optional.of(category));
        when(products.save(org.mockito.ArgumentMatchers.any(Product.class))).thenAnswer(i -> i.getArgument(0));

        ProductResponse r = service.create(new ProductRequest(1L, "Mouse", null, new BigDecimal("50000"), 5));

        assertThat(r.name()).isEqualTo("Mouse");
        assertThat(r.stock()).isEqualTo(5);
    }

    @Test
    void createFailsWhenCategoryMissing() {
        when(categories.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new ProductRequest(9L, "Mouse", null, BigDecimal.ONE, 1)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void negativeStockIsRejected() {
        Product p = new Product(category, "Mouse", null, BigDecimal.ONE, 3);
        when(products.findById(1L)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.updateStock(1L, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThat(p.getStock()).isEqualTo(3);
    }

    @Test
    void deactivateKeepsRowButMarksInactive() {
        Product p = new Product(category, "Mouse", null, BigDecimal.ONE, 3);
        when(products.findById(1L)).thenReturn(Optional.of(p));

        service.deactivate(1L);

        assertThat(p.isActive()).isFalse();
    }

    @Test
    void getInactiveOrMissingIsNotFound() {
        when(products.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }
}
