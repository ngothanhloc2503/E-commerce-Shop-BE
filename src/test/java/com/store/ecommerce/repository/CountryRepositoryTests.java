package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = false)
public class CountryRepositoryTests {

    @Autowired
    private CountryRepository countryRepository;

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
