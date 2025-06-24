package com.template.service.impl;

import com.template.entity.Code;
import com.template.repository.CodeRepository;
import com.template.service.CodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CodeServiceImpl implements CodeService {
    private final CodeRepository repository;

    @Override
    public Code createOrUpdate(Code code) {
        // Auto-assign display order if not set
        if (code.getDisplayOrder() == null || code.getDisplayOrder() == 0) {
            Integer maxOrder = repository.findMaxDisplayOrder();
            code.setDisplayOrder(maxOrder + 1);
        }
        
        // Resolve parentCode by keycode if present
        if (code.getParentCode() != null && code.getParentCode().getKeycode() != null) {
            Optional<Code> parent = repository.findBykeycode(code.getParentCode().getKeycode());
            parent.ifPresent(code::setParentCode);
        }
        // If keycode exists, update; else create new
        Optional<Code> existing = repository.findBykeycode(code.getKeycode());
        if (existing.isPresent()) {
            Code toUpdate = existing.get();
            toUpdate.setValuekey(code.getValuekey());
            toUpdate.setCategory(code.getCategory());
            toUpdate.setParentCode(code.getParentCode());
            toUpdate.setDescription(code.getDescription());
            toUpdate.setIsActive(code.getIsActive());
            // Only update display order if explicitly provided
            if (code.getDisplayOrder() != null && !code.getDisplayOrder().equals(toUpdate.getDisplayOrder())) {
                return moveCodeToPosition(toUpdate.getId(), code.getDisplayOrder());
            }
            return repository.save(toUpdate);
        } else {
            return repository.save(code);
        }
    }

    @Override
    public Optional<Code> getBykeycode(String keycode) {
        return repository.findBykeycode(keycode);
    }

    @Override
    public List<Code> getByParentCode(String parentkeycode) {
        if (parentkeycode == null) {
            return repository.findByParentCode(null);
        }
        Optional<Code> parent = repository.findBykeycode(parentkeycode);
        return parent.map(repository::findByParentCode).orElse(List.of());
    }

    @Override
    public List<Code> getAll() {
        return repository.findAll();
    }

    @Override
    public void deleteBykeycode(String keycode) {
        repository.findBykeycode(keycode).ifPresent(code -> {
            // Adjust orders of subsequent codes
            List<Code> codesToAdjust = repository.findCodesWithOrderGreaterThan(code.getDisplayOrder());
            codesToAdjust.forEach(c -> c.setDisplayOrder(c.getDisplayOrder() - 1));
            repository.saveAll(codesToAdjust);
            repository.delete(code);
            log.info("Deleted code: {} and adjusted subsequent code orders", code.getKeycode());
        });
    }

    @Override
    public Optional<Code> getById(Long id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        Code code = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + id));
        
        // Adjust orders of subsequent codes
        List<Code> codesToAdjust = repository.findCodesWithOrderGreaterThan(code.getDisplayOrder());
        codesToAdjust.forEach(c -> c.setDisplayOrder(c.getDisplayOrder() - 1));
        repository.saveAll(codesToAdjust);
        
        repository.deleteById(id);
        log.info("Deleted code: {} and adjusted subsequent code orders", code.getKeycode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Code> getAllCodesOrdered() {
        return repository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Code> getActiveCodesOrdered() {
        return repository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Code> getRootCodesOrdered() {
        return repository.findRootCodesOrderedByDisplayOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Code> getCodesByParentOrdered(String parentkeycode) {
        if (parentkeycode == null) {
            return repository.findRootCodesOrderedByDisplayOrder();
        }
        Optional<Code> parent = repository.findBykeycode(parentkeycode);
        return parent.map(repository::findByParentCodeOrderedByDisplayOrder).orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Code> getCodesByCategoryOrdered(String category) {
        return repository.findByCategoryOrderedByDisplayOrder(category);
    }

    @Override
    public Code moveCodeUp(Long codeId) {
        Code code = repository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
        
        if (code.getDisplayOrder() <= 1) {
            log.warn("Code {} is already at the top position", code.getKeycode());
            return code;
        }
        
        return moveCodeToPosition(codeId, code.getDisplayOrder() - 1);
    }

    @Override
    public Code moveCodeDown(Long codeId) {
        Code code = repository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
        
        Integer maxOrder = repository.findMaxDisplayOrder();
        if (code.getDisplayOrder() >= maxOrder) {
            log.warn("Code {} is already at the bottom position", code.getKeycode());
            return code;
        }
        
        return moveCodeToPosition(codeId, code.getDisplayOrder() + 1);
    }

    @Override
    public Code moveCodeToPosition(Long codeId, Integer newPosition) {
        Code code = repository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
        
        Integer currentPosition = code.getDisplayOrder();
        
        if (currentPosition.equals(newPosition)) {
            return code; // No change needed
        }
        
        List<Code> allCodes = repository.findAllByOrderByDisplayOrderAsc();
        
        // Remove the code from its current position
        allCodes.removeIf(c -> c.getId().equals(codeId));
        
        // Insert at new position (adjust for 0-based index)
        int insertIndex = Math.max(0, Math.min(newPosition - 1, allCodes.size()));
        allCodes.add(insertIndex, code);
        
        // Reassign all display orders
        for (int i = 0; i < allCodes.size(); i++) {
            allCodes.get(i).setDisplayOrder(i + 1);
        }
        
        repository.saveAll(allCodes);
        log.info("Moved code {} from position {} to position {}", code.getKeycode(), currentPosition, newPosition);
        
        return code;
    }

    @Override
    public Code insertCodeAtPosition(Code code, Integer position) {
        // First, increment orders of existing codes at and after the position
        repository.incrementOrdersFrom(position);
        
        // Set the display order and save
        code.setDisplayOrder(position);
        Code savedCode = repository.save(code);
        
        log.info("Inserted code {} at position {}", code.getKeycode(), position);
        return savedCode;
    }

    @Override
    public void reorderCodes(List<Long> codeIds) {
        for (int i = 0; i < codeIds.size(); i++) {
            Long codeId = codeIds.get(i);
            Code code = repository.findById(codeId)
                    .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
            
            code.setDisplayOrder(i + 1);
        }
        
        // Save all at once
        List<Code> codesToUpdate = codeIds.stream()
                .map(id -> repository.findById(id).orElseThrow())
                .toList();
        
        repository.saveAll(codesToUpdate);
        log.info("Reordered {} codes based on provided sequence", codeIds.size());
    }

    @Override
    public void activateCode(Long codeId) {
        Code code = repository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
        
        code.setIsActive(true);
        repository.save(code);
        log.info("Activated code: {}", code.getKeycode());
    }

    @Override
    public void deactivateCode(Long codeId) {
        Code code = repository.findById(codeId)
                .orElseThrow(() -> new IllegalArgumentException("Code not found with id: " + codeId));
        
        code.setIsActive(false);
        repository.save(code);
        log.info("Deactivated code: {}", code.getKeycode());
    }
}