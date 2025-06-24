package com.template.controller;

import com.template.entity.Code;
import com.template.service.CodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/location-codes")
@RequiredArgsConstructor
@Validated
public class CodeController {
    private final CodeService codeService;

    // Create or update a code (country, state, city)
    @PostMapping
    public ResponseEntity<Code> createOrUpdate(@Valid @RequestBody Code code) {
        return ResponseEntity.ok(codeService.createOrUpdate(code));
    }

    // Get a code by keycode
    @GetMapping("/{keycode}")
    public ResponseEntity<Code> getBykeycode(@PathVariable @NotBlank(message = "Keycode is required") String keycode) {
        Optional<Code> code = codeService.getBykeycode(keycode);
        return code.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // Get code by ID
    @GetMapping("/id/{id}")
    public ResponseEntity<Code> getById(@PathVariable @Positive(message = "ID must be positive") Long id) {
        return codeService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get codes by parent (e.g., get all states for a country, all cities for a state) - ordered
    @GetMapping("/parent/{parentKeycode}")
    public List<Code> getByParent(@PathVariable @NotBlank(message = "Parent keycode is required") String parentKeycode) {
        return codeService.getCodesByParentOrdered(parentKeycode);
    }

    // List all codes (countries, states, cities, etc.) - ordered
    @GetMapping
    public List<Code> getAll() {
        return codeService.getAllCodesOrdered();
    }

    // Get active codes only - ordered
    @GetMapping("/active")
    public List<Code> getActiveCodes() {
        return codeService.getActiveCodesOrdered();
    }

    // Get root codes only (no parent) - ordered
    @GetMapping("/root")
    public List<Code> getRootCodes() {
        return codeService.getRootCodesOrdered();
    }

    // Get codes by category - ordered
    @GetMapping("/category/{category}")
    public List<Code> getByCategory(@PathVariable @NotBlank(message = "Category is required") String category) {
        return codeService.getCodesByCategoryOrdered(category);
    }

    // Update code
    @PutMapping("/id/{id}")
    public ResponseEntity<Code> updateCode(@PathVariable @Positive(message = "ID must be positive") Long id, @Valid @RequestBody Code code) {
        try {
            Optional<Code> existingCode = codeService.getById(id);
            if (existingCode.isPresent()) {
                code.setId(id);
                return ResponseEntity.ok(codeService.createOrUpdate(code));
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete by keycode
    @DeleteMapping("/{keycode}")
    public ResponseEntity<Void> deleteBykeycode(@PathVariable @NotBlank(message = "Keycode is required") String keycode) {
        codeService.deleteBykeycode(keycode);
        return ResponseEntity.noContent().build();
    }

    // Delete by ID
    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable @Positive(message = "ID must be positive") Long id) {
        try {
            codeService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === Code Ordering Endpoints ===

    // Move code up by one position
    @PutMapping("/id/{id}/move-up")
    public ResponseEntity<Code> moveCodeUp(@PathVariable @Positive(message = "ID must be positive") Long id) {
        try {
            return ResponseEntity.ok(codeService.moveCodeUp(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Move code down by one position
    @PutMapping("/id/{id}/move-down")
    public ResponseEntity<Code> moveCodeDown(@PathVariable @Positive(message = "ID must be positive") Long id) {
        try {
            return ResponseEntity.ok(codeService.moveCodeDown(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Move code to specific position
    @PutMapping("/id/{id}/move-to/{position}")
    public ResponseEntity<Code> moveCodeToPosition(@PathVariable @Positive(message = "ID must be positive") Long id, @PathVariable @PositiveOrZero(message = "Position must be non-negative") Integer position) {
        try {
            return ResponseEntity.ok(codeService.moveCodeToPosition(id, position));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Bulk reorder codes
    @PutMapping("/reorder")
    public ResponseEntity<String> reorderCodes(@RequestBody @NotEmpty(message = "Code IDs list cannot be empty") List<@Positive(message = "Each ID must be positive") Long> codeIds) {
        try {
            codeService.reorderCodes(codeIds);
            return ResponseEntity.ok("Codes reordered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid code IDs provided");
        }
    }

    // Activate/Deactivate code
    @PutMapping("/id/{id}/activate")
    public ResponseEntity<String> activateCode(@PathVariable @Positive(message = "ID must be positive") Long id) {
        try {
            codeService.activateCode(id);
            return ResponseEntity.ok("Code activated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/id/{id}/deactivate")
    public ResponseEntity<String> deactivateCode(@PathVariable @Positive(message = "ID must be positive") Long id) {
        try {
            codeService.deactivateCode(id);
            return ResponseEntity.ok("Code deactivated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}