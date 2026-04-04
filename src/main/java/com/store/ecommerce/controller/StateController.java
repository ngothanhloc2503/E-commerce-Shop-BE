package com.store.ecommerce.controller;

import com.store.ecommerce.dto.StateDTO;
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

    @PostMapping("")
    public ResponseEntity<?> saveState(@RequestPart(name = "state") StateDTO stateDTO) {

        return ResponseEntity.ok(stateService.saveState(stateDTO));
    }

    @DeleteMapping("/{stateId}")
    public ResponseEntity<?> deleteStateByID(@PathVariable("stateId") Long id) {

        stateService.deleteStateByID(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
