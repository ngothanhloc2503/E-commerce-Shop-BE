package com.store.ecommerce.repository;

import com.store.ecommerce.entity.ShippingRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("ShippingRateRepository Integration Tests")
class ShippingRateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ShippingRateRepository shippingRateRepository;

    @BeforeEach
    void setUp() {
        // Create sample shipping rates
        persistShippingRate("United States", "California", 5.00f, 3, true);
        persistShippingRate("United States", "New York", 6.50f, 5, true);
        persistShippingRate("United States", "Texas", 4.00f, 2, false);
        persistShippingRate("Vietnam", "Ho Chi Minh", 2.00f, 1, true);
        persistShippingRate("Vietnam", "Ha Noi", 2.50f, 2, true);
    }

    // ======================== FIND ALL BY KEYWORD (PAGEABLE) ========================

    @Test
    @DisplayName("Should find shipping rates by keyword matching country")
    void findAll_KeywordPageable_MatchCountry() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("Vietnam", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(s ->
                s.getCountry().contains("Vietnam"));
    }

    @Test
    @DisplayName("Should find shipping rates by keyword matching state")
    void findAll_KeywordPageable_MatchState() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("California", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getState()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find shipping rates by partial keyword")
    void findAll_KeywordPageable_PartialMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — "York" should match "New York"
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("York", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getState()).isEqualTo("New York");
    }

    @Test
    @DisplayName("Should match keyword against both country and state")
    void findAll_KeywordPageable_MatchCountryOrState() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — "United" matches country "United States" for all US rates
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("United", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(3); // California, New York, Texas
        assertThat(result.getContent()).allMatch(s ->
                s.getCountry().contains("United"));
    }

    @Test
    @DisplayName("Should return paginated results")
    void findAll_KeywordPageable_Pagination() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 2);

        // Act — "United" matches 3 US rates, but page size is 2
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("United", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    @DisplayName("Should return second page of keyword search")
    void findAll_KeywordPageable_SecondPage() {
        // Arrange
        PageRequest pageable = PageRequest.of(1, 2);

        // Act
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("United", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1); // 3 total - 2 from page 1
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.isLast()).isTrue();
    }

    @Test
    @DisplayName("Should return empty page when keyword matches nothing")
    void findAll_KeywordPageable_NoMatch() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("Antarctica", pageable);

        // Assert
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should return all rates when keyword is empty string")
    void findAll_KeywordPageable_EmptyKeyword() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 10);

        // Act — empty keyword matches everything (LIKE %%)
        Page<ShippingRate> result = shippingRateRepository.searchByKeyword("", pageable);

        // Assert
        assertThat(result.getContent()).hasSize(5); // all from setUp
    }

    // ======================== FIND ALL BY KEYWORD (SORT) ========================

    @Test
    @DisplayName("Should find shipping rates by keyword with sort ascending")
    void findAll_KeywordSort_Ascending() {
        // Arrange
        Sort sort = Sort.by("state").ascending();

        // Act
        List<ShippingRate> result = shippingRateRepository.searchByKeyword("United", sort);

        // Assert — should be sorted by state ascending
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ShippingRate::getState)
                .containsExactly("California", "New York", "Texas");
    }

    @Test
    @DisplayName("Should find shipping rates by keyword with sort descending")
    void findAll_KeywordSort_Descending() {
        // Arrange
        Sort sort = Sort.by("state").descending();

        // Act
        List<ShippingRate> result = shippingRateRepository.searchByKeyword("United", sort);

        // Assert — should be sorted by state descending
        assertThat(result).hasSize(3);
        assertThat(result).extracting(ShippingRate::getState)
                .containsExactly("Texas", "New York", "California");
    }

    @Test
    @DisplayName("Should find by keyword matching state with sort")
    void findAll_KeywordSort_MatchState() {
        // Arrange
        Sort sort = Sort.by("country").ascending();

        // Act — "Chi" matches "Ho Chi Minh" in state
        List<ShippingRate> result = shippingRateRepository.searchByKeyword("Chi", sort);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result).anyMatch(s -> s.getState().equals("Ho Chi Minh"));
    }

    @Test
    @DisplayName("Should return empty list when keyword matches nothing with sort")
    void findAll_KeywordSort_NoMatch() {
        // Arrange
        Sort sort = Sort.by("state").ascending();

        // Act
        List<ShippingRate> result = shippingRateRepository.searchByKeyword("XYZNonExistent", sort);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should sort by rate field")
    void findAll_KeywordSort_SortByRate() {
        // Arrange
        Sort sort = Sort.by("rate").ascending();

        // Act — match all US rates, sorted by rate
        List<ShippingRate> result = shippingRateRepository.searchByKeyword("United", sort);

        // Assert
        assertThat(result).hasSize(3);
        List<Float> rates = result.stream().map(ShippingRate::getRate).toList();
        assertThat(rates).isSorted();
    }

    // ======================== UPDATE COD SUPPORTED ========================

    @Test
    @DisplayName("Should enable COD support for shipping rate")
    void updateCODSupported_Enable() {
        // Arrange — Texas has codSupported = false
        ShippingRate texasRate = findRateByState("Texas");
        assertThat(texasRate.isCodSupported()).isFalse();

        // Act
        shippingRateRepository.updateCODSupported(texasRate.getId(), true);

        // Assert
        entityManager.flush();
        entityManager.clear();

        ShippingRate updated = entityManager.find(ShippingRate.class, texasRate.getId());
        assertThat(updated.isCodSupported()).isTrue();
    }

    @Test
    @DisplayName("Should disable COD support for shipping rate")
    void updateCODSupported_Disable() {
        // Arrange — California has codSupported = true
        ShippingRate caRate = findRateByState("California");
        assertThat(caRate.isCodSupported()).isTrue();

        // Act
        shippingRateRepository.updateCODSupported(caRate.getId(), false);

        // Assert
        entityManager.flush();
        entityManager.clear();

        ShippingRate updated = entityManager.find(ShippingRate.class, caRate.getId());
        assertThat(updated.isCodSupported()).isFalse();
    }

    @Test
    @DisplayName("Should keep COD enabled when already enabled")
    void updateCODSupported_AlreadyEnabled() {
        // Arrange
        ShippingRate caRate = findRateByState("California");
        assertThat(caRate.isCodSupported()).isTrue();

        // Act — enable again
        shippingRateRepository.updateCODSupported(caRate.getId(), true);

        // Assert
        entityManager.flush();
        entityManager.clear();

        ShippingRate updated = entityManager.find(ShippingRate.class, caRate.getId());
        assertThat(updated.isCodSupported()).isTrue();
    }

    // ======================== FIND BY COUNTRY AND STATE ========================

    @Test
    @DisplayName("Should find shipping rate by country and state")
    void findByCountryAndState_Found() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "United States", "California");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCountry()).isEqualTo("United States");
        assertThat(result.get().getState()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find shipping rate ignoring case for country and state")
    void findByCountryAndState_IgnoreCase() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "united states", "california");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getState()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find shipping rate with uppercase country and state")
    void findByCountryAndState_Uppercase() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "UNITED STATES", "CALIFORNIA");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getState()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find Vietnamese shipping rate ignoring case")
    void findByCountryAndState_VietnamIgnoreCase() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "vietnam", "ho chi minh");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getCountry()).isEqualTo("Vietnam");
        assertThat(result.get().getState()).isEqualTo("Ho Chi Minh");
    }

    @Test
    @DisplayName("Should return null when country not found")
    void findByCountryAndState_CountryNotFound() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "Germany", "California");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return null when state not found")
    void findByCountryAndState_StateNotFound() {
        // Act
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "United States", "Ohio");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not return state from different country with same name")
    void findByCountryAndState_SameStateDifferentCountry() {
        // Arrange — create a "California" in Vietnam
        persistShippingRate("Vietnam", "California", 3.00f, 2, true);

        // Act — search for California in United States
        Optional<ShippingRate> result = shippingRateRepository.findByCountryAndState(
                "United States", "California");

        // Assert — should find US version
        assertThat(result).isPresent();
        assertThat(result.get().getCountry()).isEqualTo("United States");
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve shipping rate")
    void save_AndFindById() {
        // Arrange
        ShippingRate rate = new ShippingRate();
        rate.setCountry("Japan");
        rate.setState("Tokyo");
        rate.setRate(8.00f);
        rate.setDays(3);
        rate.setCodSupported(true);

        // Act
        ShippingRate saved = shippingRateRepository.save(rate);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getCountry()).isEqualTo("Japan");
        assertThat(saved.getState()).isEqualTo("Tokyo");
    }

    @Test
    @DisplayName("Should delete shipping rate by id")
    void deleteById_Success() {
        // Arrange
        ShippingRate rate = findRateByState("Texas");
        Long rateId = rate.getId();

        // Act
        shippingRateRepository.deleteById(rateId);

        // Assert
        assertThat(entityManager.find(ShippingRate.class, rateId)).isNull();
    }

    @Test
    @DisplayName("Should count shipping rates correctly")
    void count_Success() {
        // 5 from setUp
        assertThat(shippingRateRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find all shipping rates")
    void findAll_Success() {
        List<ShippingRate> rates = shippingRateRepository.findAll();
        assertThat(rates).hasSize(5);
    }

    // ======================== HELPER METHODS ========================

    private ShippingRate persistShippingRate(String country, String state,
                                             float rate, int days, boolean codSupported) {
        ShippingRate shippingRate = new ShippingRate();
        shippingRate.setCountry(country);
        shippingRate.setState(state);
        shippingRate.setRate(rate);
        shippingRate.setDays(days);
        shippingRate.setCodSupported(codSupported);
        return entityManager.persistAndFlush(shippingRate);
    }

    private ShippingRate findRateByState(String stateName) {
        return entityManager.getEntityManager()
                .createQuery("SELECT s FROM ShippingRate s WHERE s.state = :state", ShippingRate.class)
                .setParameter("state", stateName)
                .getSingleResult();
    }
}
