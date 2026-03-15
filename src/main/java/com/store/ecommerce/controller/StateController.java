package com.store.ecommerce.controller;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/states")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StateController {
    private final StateService stateService;

    @GetMapping("")
    public ResponseEntity<?> getListStatesByCountryName(@RequestParam(name = "country") String countryName) {
        try {
            return ResponseEntity.ok(stateService.listStatesByCountryName(countryName));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("")
    public ResponseEntity<?> saveState(@RequestPart(name = "state") StateDTO stateDTO) {
        try {
            return ResponseEntity.ok(stateService.saveState(stateDTO));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{stateId}")
    public ResponseEntity<?> deleteStateByID(@PathVariable("stateId") Long id) {
        try {
            stateService.deleteStateByID(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
