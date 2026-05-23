package com.store.ecommerce.repository;

import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.State;
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
@DisplayName("StateRepository Integration Tests")
class StateRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StateRepository stateRepository;

    private Country usa;
    private Country vietnam;

    @BeforeEach
    void setUp() {
        // Persist countries first (State depends on Country)
        usa = new Country();
        usa.setName("United States");
        usa.setCode("US");
        entityManager.persistAndFlush(usa);

        vietnam = new Country();
        vietnam.setName("Vietnam");
        vietnam.setCode("VN");
        entityManager.persistAndFlush(vietnam);

        // Persist some states
        persistState("California", usa);
        persistState("New York", usa);
        persistState("Texas", usa);
        persistState("Florida", usa);

        persistState("Ha Noi", vietnam);
        persistState("Ho Chi Minh", vietnam);
        persistState("Da Nang", vietnam);
    }

    // ======================== FIND BY COUNTRY ORDER BY NAME ASC ========================

    @Test
    @DisplayName("Should find states by country sorted by name ascending")
    void findByCountryOrderByNameAsc_Success() {
        // Act
        List<State> states = stateRepository.findByCountryOrderByNameAsc(usa);

        // Assert
        assertThat(states).hasSize(4);
        assertThat(states).extracting(State::getName)
                .containsExactly("California", "Florida", "New York", "Texas");
    }

    @Test
    @DisplayName("Should find states for Vietnam sorted by name ascending")
    void findByCountryOrderByNameAsc_Vietnam() {
        // Act
        List<State> states = stateRepository.findByCountryOrderByNameAsc(vietnam);

        // Assert
        assertThat(states).hasSize(3);
        assertThat(states).extracting(State::getName)
                .containsExactly("Da Nang", "Ha Noi", "Ho Chi Minh");
    }

    @Test
    @DisplayName("Should return empty list when country has no states")
    void findByCountryOrderByNameAsc_NoStates() {
        // Arrange
        Country emptyCountry = new Country();
        emptyCountry.setName("Antarctica");
        emptyCountry.setCode("AQ");
        entityManager.persistAndFlush(emptyCountry);

        // Act
        List<State> states = stateRepository.findByCountryOrderByNameAsc(emptyCountry);

        // Assert
        assertThat(states).isEmpty();
    }

    @Test
    @DisplayName("Should not mix states from different countries")
    void findByCountryOrderByNameAsc_NoCrossCountry() {
        // Act
        List<State> usStates = stateRepository.findByCountryOrderByNameAsc(usa);
        List<State> vnStates = stateRepository.findByCountryOrderByNameAsc(vietnam);

        // Assert — no overlap
        assertThat(usStates).noneMatch(s -> s.getCountry().equals(vietnam));
        assertThat(vnStates).noneMatch(s -> s.getCountry().equals(usa));
    }

    // ======================== FIND BY COUNTRY AND NAME IGNORE CASE ========================

    @Test
    @DisplayName("Should find state by country and name ignoring case")
    void findByCountryAndNameIgnoreCase_Found() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "United States", "california");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("California");
        assertThat(found.get().getCountry().getName()).isEqualTo("United States");
    }

    @Test
    @DisplayName("Should find state with both country and name uppercase")
    void findByCountryAndNameIgnoreCase_BothUppercase() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "UNITED STATES", "CALIFORNIA");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("California");
    }

    @Test
    @DisplayName("Should find state with both country and name lowercase")
    void findByCountryAndNameIgnoreCase_BothLowercase() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "united states", "new york");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("New York");
    }

    @Test
    @DisplayName("Should find state with mixed case")
    void findByCountryAndNameIgnoreCase_MixedCase() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "vIeTnAm", "hA nOi");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ha Noi");
    }

    @Test
    @DisplayName("Should return empty when state name not found")
    void findByCountryAndNameIgnoreCase_StateNotFound() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "United States", "NonExistentState");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when country name not found")
    void findByCountryAndNameIgnoreCase_CountryNotFound() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "NonExistentCountry", "California");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when both country and state not found")
    void findByCountryAndNameIgnoreCase_BothNotFound() {
        // Act
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "XYZ", "ABC");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should not return state from different country with same name")
    void findByCountryAndNameIgnoreCase_SameStateNameDifferentCountry() {
        // Arrange — create a state named "California" in Vietnam
        persistState("California", vietnam);

        // Act — search for California in USA
        Optional<State> found = stateRepository.findByCountryAndNameIgnoreCase(
                "United States", "California");

        // Assert — should find the US one, not the Vietnam one
        assertThat(found).isPresent();
        assertThat(found.get().getCountry().getName()).isEqualTo("United States");
    }

    // ======================== CRUD BASICS ========================

    @Test
    @DisplayName("Should save and retrieve state")
    void save_AndFindById() {
        // Arrange
        State state = new State();
        state.setName("Ohio");
        state.setCountry(usa);

        // Act
        State saved = stateRepository.save(state);

        // Assert
        assertThat(saved.getId()).isGreaterThan(0);
        Optional<State> found = stateRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Ohio");
    }

    @Test
    @DisplayName("Should delete state by id")
    void deleteById_Success() {
        // Arrange
        State state = persistState("Ohio", usa);
        Long stateId = state.getId();

        // Act
        stateRepository.deleteById(stateId);

        // Assert
        Optional<State> found = stateRepository.findById(stateId);
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should count states correctly")
    void count_Success() {
        // 4 USA + 3 Vietnam = 7 from setUp
        assertThat(stateRepository.count()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should find all states")
    void findAll_Success() {
        // 4 USA + 3 Vietnam = 7 from setUp
        List<State> states = stateRepository.findAll();
        assertThat(states).hasSize(7);
    }

    // ======================== HELPER METHODS ========================

    private State persistState(String name, Country country) {
        State state = new State();
        state.setName(name);
        state.setCountry(country);
        return entityManager.persistAndFlush(state);
    }
}