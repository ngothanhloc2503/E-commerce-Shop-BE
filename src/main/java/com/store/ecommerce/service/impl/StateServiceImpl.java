package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.entity.Country;
import com.store.ecommerce.entity.State;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.repository.CountryRepository;
import com.store.ecommerce.repository.StateRepository;
import com.store.ecommerce.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StateServiceImpl implements StateService {
    private final StateRepository stateRepository;
    private final CountryRepository countryRepository;

    @Override
    public List<StateDTO> listStatesByCountryID(Long countryID) throws NotFoundException {
        if (!countryRepository.existsById(countryID)) {
            throw new NotFoundException("Could not find any country with ID: " + countryID);
        }

        List<State> states = stateRepository.findByCountryIdOrderByNameAsc(countryID);
        return states.stream().map(State::toStateDTO).toList();
    }

    @Override
    public StateDTO saveState(StateDTO stateDTO) throws NotFoundException {
        Long countryId = stateDTO.getCountryID();

        if (!countryRepository.existsById(countryId)) {
            throw new NotFoundException("Could not find any country with ID: " + countryId);
        }

        Country country = countryRepository.getReferenceById(countryId);

        State state = new State(stateDTO.getId(), stateDTO.getName(), country);
        return stateRepository.save(state).toStateDTO();
    }

    @Override
    public void deleteStateByID(Long id) throws NotFoundException {
        if (!stateRepository.existsById(id)) {
            throw new NotFoundException("Could not find any state with ID: " + id);
        }

        stateRepository.deleteById(id);
    }

    @Override
    public List<StateDTO> listStatesByCountryName(String countryName) throws NotFoundException {
        Country country = countryRepository.findByNameIgnoreCase(countryName).orElseThrow(
                () -> new NotFoundException("Could not find any country with name: " + countryName));

        List<State> states = stateRepository.findByCountryOrderByNameAsc(country);
        return states.stream().map(State::toStateDTO).toList();
    }
}
