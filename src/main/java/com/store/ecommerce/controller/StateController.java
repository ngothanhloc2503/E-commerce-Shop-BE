package com.store.ecommerce.controller;

import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("StateController")
@RequestMapping("/api/states")
@RequiredArgsConstructor
public class StateController {
    private final StateService stateService;

    @GetMapping("/list-by-country/{countryName}")
    public ResponseEntity<?> getListStatesByCountryID(@PathVariable("countryName") String countryName) {
        try {
            return ResponseEntity.ok(stateService.listStatesByCountryName(countryName));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
