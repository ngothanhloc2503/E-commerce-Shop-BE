package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback(value = true)
public class StateRepositoryTests {
    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void testCreateState() {
        Integer countryId = 1;
        Country country = testEntityManager.find(Country.class, countryId);

        State state = stateRepository.save(new State("San Francisco", country));

        assertThat(state).isNotNull();
        assertThat(state.getId()).isGreaterThan(0);
    }

    @Test
    public void findByCountryOrderByNameAsc() {
        Integer countryId = 1;
        Country country = testEntityManager.find(Country.class, countryId);

        List<State> listStates = stateRepository.findByCountryOrderByNameAsc(country);

        listStates.forEach(System.out::println);

        assertThat(listStates).size().isGreaterThan(0);
    }
}
