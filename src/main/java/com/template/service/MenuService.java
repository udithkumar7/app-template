package com.template.service;

import com.template.entity.Menu;
import java.util.List;
import java.util.Optional;

public interface MenuService {
    
    // Basic CRUD operations
    Menu createMenu(Menu menu);
    Optional<Menu> getMenuById(Long id);
    Menu updateMenu(Long id, Menu menu);
    void deleteMenu(Long id);
    
    // Ordering operations
    List<Menu> getAllMenusOrdered();
    List<Menu> getActiveMenusOrdered();
    List<Menu> getRootMenusOrdered();
    List<Menu> getSubMenusOrdered(Long parentId);
    
    // Menu ordering management
    Menu moveMenuUp(Long menuId);
    Menu moveMenuDown(Long menuId);
    Menu moveMenuToPosition(Long menuId, Integer newPosition);
    Menu insertMenuAtPosition(Menu menu, Integer position);
    
    // Bulk operations
    void reorderMenus(List<Long> menuIds); // Reorder based on provided ID sequence
    void activateMenu(Long menuId);
    void deactivateMenu(Long menuId);
} 