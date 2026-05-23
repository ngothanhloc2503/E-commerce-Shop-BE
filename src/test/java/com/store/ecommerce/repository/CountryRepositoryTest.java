package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Rollback(value = true)
@DisplayName("CountryRepository Integration Tests")
class CountryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        persistCountry("Vietnam", "VN");
        persistCountry("United States", "US");
        persistCountry("Germany", "DE");
        persistCountry("Brazil", "BR");
        persistCountry("Japan", "JP");
    }

    // ======================== FIND ALL ORDER BY NAME ASC ========================

    @Test
    @DisplayName("Should return all countries sorted by name ascending")
    void findAllByOrderByNameAsc_Sorted() {
        // Act
        List<Country> countries = countryRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(countries).hasSize(5);
        assertThat(countries).extracting(Country::getName)
                .containsExactly("Brazil", "Germany", "Japan", "United States", "Vietnam");
    }

    @Test
    @DisplayName("Should verify alphabetical order of names")
    void findAllByOrderByNameAsc_AlphabeticalOrder() {
        // Act
        List<Country> countries = countryRepository.findAllByOrderByNameAsc();

        // Assert
        List<String> names = countries.stream().map(Country::getName).toList();
        assertThat(names).isSorted();
    }

    @Test
    @DisplayName("Should return empty list when no countries exist")
    void findAllByOrderByNameAsc_Empty() {
        // Arrange
        countryRepository.deleteAll();

        // Act
        List<Country> countries = countryRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(countries).isEmpty();
    }

    @Test
    @DisplayName("Should include all persisted countries")
    void findAllByOrderByNameAsc_AllCountriesPresent() {
        // Act
        List<Country> countries = countryRepository.findAllByOrderByNameAsc();

        // Assert
        assertThat(countries).extracting(Country::getName)
                .containsExactlyInAnyOrder("Vietnam", "United States", "Germany", "Brazil", "Japan");
    }

    // ======================== FIND BY NAME IGNORE CASE ========================

    @Test
    @DisplayName("Should find country by name ignoring case - exact match")
    void findByNameIgnoreCase_ExactMatch() {
        // Act
        Optional<Country> found = countryRepository.findByNameIgnoreCase("Vietnam");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Vietnam");
        assertThat(found.get().getCode()).isEqualTo("VN");
    }

    @Test
    @DisplayName("Should find country by name ignoring case - uppercase")
    void findByNameIgnoreCase_Uppercase() {
        // Act
        Optional<Country> found = countryRepository.findByNameIgnoreCase("VIETNAM");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Vietnam");
    }

    @Test
    @DisplayName("Should find country by name ignoring case - lowercase")
    void findByNameIgnoreCase_Lowercase() {
        // Act
        Optional<Country> found = countryRepository.findByNameIgnoreCase("united states");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("United States");
    }

    @Test
    @DisplayName("Should find country by name ignoring case - mixed case")
    void findByNameIgnoreCase_MixedCase() {
        // Act
        Optional<Country> found = countryRepository.findByNameIgnoreCase("gErMaNy");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Germany");
    }

    @Test
    @DisplayName("Should return empty when country name not found")
    void findByNameIgnoreCase_NotFound() {
        // Act
        Optional<Country> found = countryRepository.findByNameIgnoreCase("Antarctica");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should not find country by partial name")
    void findByNameIgnoreCase_PartialName() {
        // Act — "Viet" is not full name, should not match
        Optional<Country> found = countryRepository.findByNameIgnoreCase("Viet");

        // Assert
        assertThat(found).isEmpty();
    }

    // ======================== FIND BY CODE ========================

    @Test
    @DisplayName("Should find country by code")
    void findByCode_Found() {
        // Act
        Optional<Country> found = countryRepository.findByCode("VN");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Vietnam");
        assertThat(found.get().getCode()).isEqualTo("VN");
    }

    @Test
    @DisplayName("Should find country by US code")
    void findByCode_US() {
        // Act
        Optional<Country> found = countryRepository.findByCode("US");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("United States");
    }

    @Test
    @DisplayName("Should find country by DE code")
    void findByCode_DE() {
        // Act
        Optional<Country> found = countryRepository.findByCode("DE");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Germany");
    }

    @Test
    @DisplayName("Should return empty when code not found")
    void findByCode_NotFound() {
        // Act
        Optional<Country> found = countryRepository.findByCode("XX");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find by exact code - case sensitive")
    void findByCode_CaseSensitive() {
        // Act — "vn" lowercase should not match "VN"
        Optional<Country> found = countryRepository.findByCode("vn");

        // Assert — JPA default is case-sensitive
        assertThat(found).isEmpty();
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve country")
    void save_AndFindById() {
        // Arrange
        Country country = new Country();
        country.setName("France");
        country.setCode("FR");

        // Act
        Country saved = countryRepository.save(country);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getName()).isEqualTo("France");
        assertThat(saved.getCode()).isEqualTo("FR");
    }

    @Test
    @DisplayName("Should update existing country")
    void save_UpdateExisting() {
        // Arrange
        Optional<Country> vietnam = countryRepository.findByNameIgnoreCase("Vietnam");
        assertThat(vietnam).isPresent();

        // Act
        vietnam.get().setCode("VNM");
        countryRepository.save(vietnam.get());

        // Assert
        entityManager.flush();
        entityManager.clear();

        Country updated = entityManager.find(Country.class, vietnam.get().getId());
        assertThat(updated.getCode()).isEqualTo("VNM");
    }

    @Test
    @DisplayName("Should delete country by id")
    void deleteById_Success() {
        // Arrange
        Optional<Country> japan = countryRepository.findByCode("JP");
        assertThat(japan).isPresent();
        Long countryId = japan.get().getId();

        // Act
        countryRepository.deleteById(countryId);

        // Assert
        assertThat(countryRepository.findById(countryId)).isEmpty();
        assertThat(countryRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("Should count countries correctly")
    void count_Success() {
        // 5 from setUp
        assertThat(countryRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should find all countries")
    void findAll_Success() {
        List<Country> countries = countryRepository.findAll();
        assertThat(countries).hasSize(5);
    }

    // ======================== HELPER METHODS ========================

    private Country persistCountry(String name, String code) {
        Country country = new Country();
        country.setName(name);
        country.setCode(code);
        return entityManager.persistAndFlush(country);
    }
}