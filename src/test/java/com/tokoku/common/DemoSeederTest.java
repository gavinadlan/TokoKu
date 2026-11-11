package com.tokoku.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.tokoku.auth.User;
import com.tokoku.auth.UserRepository;
import com.tokoku.product.*;
import com.tokoku.store.StoreService;
import com.tokoku.store.StoreSettings;

class DemoSeederTest {
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final StoreService store = mock(StoreService.class);

    private DemoSeeder seeder(String password) {
        when(categories.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        StoreSettings s = mock(StoreSettings.class);
        when(s.isConfigured()).thenReturn(true);
        when(store.current()).thenReturn(s);
        return new DemoSeeder(categories, products, users, store, new BCryptPasswordEncoder(), "Demo@Tokoku.example", password);
    }

    @Test
    void seedsProductsAndDemoUserWhenEmpty() {
        when(products.count()).thenReturn(0L);
        seeder("rahasia-demo-1").run(null);
        verify(products, times(6)).save(any(Product.class));
        verify(users).save(argThat((User u) -> u.getEmail().equals("demo@tokoku.example") && !u.getPasswordHash().equals("rahasia-demo-1")));
    }

    @Test
    void neverTouchesExistingCatalogAndSkipsUserWithoutPassword() {
        when(products.count()).thenReturn(3L);
        seeder("").run(null);
        verify(products, never()).save(any(Product.class));
        verify(users, never()).save(any(User.class));
    }
}
