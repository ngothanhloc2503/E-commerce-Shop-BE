package com.store.ecommerce.controller.staff;

import com.store.ecommerce.dto.StateDTO;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.service.StateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("ManageStateController")
@RequestMapping("/api/staff/states")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StateController {
    private final StateService stateService;

    @GetMapping("/list-by-country/{countryId}")
    public ResponseEntity<?> getListStatesByCountryID(@PathVariable("countryId") Long countryID) {
        try {
            return ResponseEntity.ok(stateService.listStatesByCountryID(countryID));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveState(@RequestPart(name = "state") StateDTO stateDTO) {
        try {
            return ResponseEntity.ok(stateService.saveState(stateDTO));
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{stateId}")
    public ResponseEntity<?> deleteStateByID(@PathVariable("stateId") Long id) {
        try {
            stateService.deleteStateByID(id);
            return new ResponseEntity<>(HttpStatus.ACCEPTED);
        } catch (NotFoundException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}
