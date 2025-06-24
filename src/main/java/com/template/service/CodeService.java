package com.template.service;

import com.template.entity.Code;
import java.util.List;
import java.util.Optional;

public interface CodeService {
    
    // Basic CRUD operations
    Code createOrUpdate(Code code);
    Optional<Code> getBykeycode(String keycode);
    Optional<Code> getById(Long id);
    List<Code> getByParentCode(String parentkeycode); // null for top-level
    List<Code> getAll();
    void deleteBykeycode(String keycode);
    void deleteById(Long id);
    
    // Ordering operations
    List<Code> getAllCodesOrdered();
    List<Code> getActiveCodesOrdered();
    List<Code> getRootCodesOrdered();
    List<Code> getCodesByParentOrdered(String parentkeycode);
    List<Code> getCodesByCategoryOrdered(String category);
    
    // Code ordering management
    Code moveCodeUp(Long codeId);
    Code moveCodeDown(Long codeId);
    Code moveCodeToPosition(Long codeId, Integer newPosition);
    Code insertCodeAtPosition(Code code, Integer position);
    
    // Bulk operations
    void reorderCodes(List<Long> codeIds); // Reorder based on provided ID sequence
    void activateCode(Long codeId);
    void deactivateCode(Long codeId);
} 