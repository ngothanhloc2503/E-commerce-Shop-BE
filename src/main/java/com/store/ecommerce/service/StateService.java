package com.store.ecommerce.service;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.exception.NotFoundException;

import java.util.List;

public interface StateService {
    List<StateDTO> listStatesByCountryID(Long countryID) throws NotFoundException;

    StateDTO saveState(StateDTO stateDTO) throws NotFoundException;

    void deleteStateByID(Long id) throws NotFoundException;

    List<StateDTO> listStatesByCountryName(String countryName) throws NotFoundException;
}
