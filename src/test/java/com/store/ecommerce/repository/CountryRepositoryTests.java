package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
@RequiredArgsConstructor
public class CountryRepositoryTests {
    private final CountryRepository countryRepository;

    @Test
    public void testCreateCountry() {
        Country country = countryRepository.save(new Country("United Kingdom", "US"));

        assertThat(country).isNotNull();
        assertThat(country.getId()).isGreaterThan(0);
    }

    @Test
    public void testFindAllByOrderByNameAsc() {
        List<Country> countries = countryRepository.findAllByOrderByNameAsc();
        countries.forEach(System.out::println);

        assertThat(countries).size().isGreaterThan(0);
    }
}
