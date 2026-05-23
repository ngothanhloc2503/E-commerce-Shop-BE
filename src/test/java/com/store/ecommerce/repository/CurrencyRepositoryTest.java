package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("CurrencyRepository Integration Tests")
class CurrencyRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CurrencyRepository currencyRepository;

    @BeforeEach
    void setUp() {
        persistCurrency("Vietnamese Dong", "VND", "$");
        persistCurrency("US Dollar", "USD", "$");
        persistCurrency("Euro", "EUR", "€");
        persistCurrency("British Pound", "GBP", "£");
        persistCurrency("Japanese Yen", "JPY", "¥");
    }

    // ======================== FIND ALL ORDER BY NAME ASC ========================

    @Test
    @DisplayName("Should return all currencies sorted by name ascending")
    void findAllByOrderByNameAsc_Sorted() {
        // Act
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(currencies).hasSize(5);
        assertThat(currencies).extracting(Currency::getName)
                .containsExactly("British Pound", "Euro", "Japanese Yen", "US Dollar", "Vietnamese Dong");
    }

    @Test
    @DisplayName("Should verify alphabetical order of names")
    void findAllByOrderByNameAsc_AlphabeticalOrder() {
        // Act
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();

        // Assert — extract names and verify sorted
        List<String> names = currencies.stream().map(Currency::getName).toList();
        assertThat(names).isSorted();
    }

    @Test
    @DisplayName("Should return empty list when no currencies exist")
    void findAllByOrderByNameAsc_Empty() {
        // Arrange
        currencyRepository.deleteAll();

        // Act
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(currencies).isEmpty();
    }

    @Test
    @DisplayName("Should include all persisted currencies")
    void findAllByOrderByNameAsc_AllCurrenciesPresent() {
        // Act
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(currencies).extracting(Currency::getName)
                .containsExactlyInAnyOrder("US Dollar", "Euro", "British Pound", "Japanese Yen", "Vietnamese Dong");
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve currency")
    void save_AndFindById() {
        // Arrange
        Currency currency = new Currency();
        currency.setName("Canadian Dollar");
        currency.setSymbol("C$");
        currency.setCode("CAD");

        // Act
        Currency saved = currencyRepository.save(currency);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("Canadian Dollar");
        assertThat(saved.getCode()).isEqualTo("CAD");
    }

    @Test
    @DisplayName("Should update existing currency")
    void save_UpdateExisting() {
        // Arrange
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();
        Currency euro = currencies.stream()
                .filter(c -> c.getName().equals("Euro"))
                .findFirst()
                .orElseThrow();

        // Act
        euro.setSymbol("€€");
        currencyRepository.save(euro);

        // Assert
        entityManager.flush();
        entityManager.clear();

        Currency updated = entityManager.find(Currency.class, euro.getId());
        assertThat(updated.getSymbol()).isEqualTo("€€");
    }

    @Test
    @DisplayName("Should delete currency by id")
    void deleteById_Success() {
        // Arrange
        List<Currency> currencies = currencyRepository.findAllByOrderByNameAsc();
        Long currencyId = currencies.get(0).getId();

        // Act
        currencyRepository.deleteById(currencyId);

        // Assert
        assertThat(currencyRepository.findById(currencyId)).isEmpty();
        assertThat(currencyRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should count currencies correctly")
    void count_Success() {
        // 5 from setUp
        assertThat(currencyRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find all currencies")
    void findAll_Success() {
        List<Currency> currencies = currencyRepository.findAll();
        assertThat(currencies).hasSize(5);
    }

    // ======================== HELPER METHODS ========================

    private Currency persistCurrency(String name, String code, String symbol) {
        Currency currency = new Currency();
        currency.setName(name);
        currency.setCode(code);
        currency.setSymbol(symbol);
        return entityManager.persistAndFlush(currency);
    }
}
