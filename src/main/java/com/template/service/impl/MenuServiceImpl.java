package com.template.service.impl;

import com.template.entity.Menu;
import com.template.repository.MenuRepository;
import com.template.service.MenuService;
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
public class MenuServiceImpl implements MenuService {
    
    private final MenuRepository menuRepository;

    @Override
    public Menu createMenu(Menu menu) {
        log.info("MenuService: Creating menu '{}' with parent: {}", 
                menu.getName(), 
                menu.getParentMenu() != null ? menu.getParentMenu().getId() : "null");
        
        if (menu.getDisplayOrder() == null || menu.getDisplayOrder() <= 0) {
            // Auto-assign next available order (highest + 1)
            Integer maxOrder = menuRepository.findMaxDisplayOrder();
            menu.setDisplayOrder(maxOrder + 1);
            log.info("Auto-assigned display order {} to menu: {}", maxOrder + 1, menu.getName());
        } else {
            // If a specific order is provided, insert at that position
            // First increment orders of existing menus at and after the position
            menuRepository.incrementOrdersFrom(menu.getDisplayOrder());
            log.info("Inserted menu {} at specific position {}", menu.getName(), menu.getDisplayOrder());
        }
        
        Menu savedMenu = menuRepository.save(menu);
        log.info("MenuService: Saved menu '{}' with ID: {} and parent: {}", 
                savedMenu.getName(), 
                savedMenu.getId(),
                savedMenu.getParentMenu() != null ? savedMenu.getParentMenu().getId() : "null");
        
        return savedMenu;
    }

    @Override
    public Optional<Menu> getMenuById(Long id) {
        return menuRepository.findById(id);
    }

    @Override
    public Menu updateMenu(Long id, Menu menuUpdates) {
        Menu existingMenu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + id));
        
        // Update fields but preserve order unless explicitly changed
        existingMenu.setName(menuUpdates.getName());
        existingMenu.setPath(menuUpdates.getPath());
        existingMenu.setDescription(menuUpdates.getDescription());
        existingMenu.setIcon(menuUpdates.getIcon());
        existingMenu.setIsActive(menuUpdates.getIsActive());
        existingMenu.setParentMenu(menuUpdates.getParentMenu());
        
        if (menuUpdates.getDisplayOrder() != null && 
            !menuUpdates.getDisplayOrder().equals(existingMenu.getDisplayOrder())) {
            return moveMenuToPosition(id, menuUpdates.getDisplayOrder());
        }
        
        return menuRepository.save(existingMenu);
    }

    @Override
    public void deleteMenu(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + id));
        
        // Adjust orders of subsequent menus
        List<Menu> menusToAdjust = menuRepository.findMenusWithOrderGreaterThan(menu.getDisplayOrder());
        menusToAdjust.forEach(m -> m.setDisplayOrder(m.getDisplayOrder() - 1));
        menuRepository.saveAll(menusToAdjust);
        
        menuRepository.deleteById(id);
        log.info("Deleted menu: {} and adjusted subsequent menu orders", menu.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getAllMenusOrdered() {
        return menuRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getActiveMenusOrdered() {
        return menuRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getRootMenusOrdered() {
        return menuRepository.findRootMenusOrderedByDisplayOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Menu> getSubMenusOrdered(Long parentId) {
        return menuRepository.findSubMenusOrderedByDisplayOrder(parentId);
    }

    @Override
    public Menu moveMenuUp(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
        
        if (menu.getDisplayOrder() <= 1) {
            log.warn("Menu {} is already at the top position", menu.getName());
            return menu;
        }
        
        return moveMenuToPosition(menuId, menu.getDisplayOrder() - 1);
    }

    @Override
    public Menu moveMenuDown(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
        
        Integer maxOrder = menuRepository.findMaxDisplayOrder();
        if (menu.getDisplayOrder() >= maxOrder) {
            log.warn("Menu {} is already at the bottom position", menu.getName());
            return menu;
        }
        
        return moveMenuToPosition(menuId, menu.getDisplayOrder() + 1);
    }

    @Override
    public Menu moveMenuToPosition(Long menuId, Integer newPosition) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
        
        Integer currentPosition = menu.getDisplayOrder();
        
        if (currentPosition.equals(newPosition)) {
            return menu; // No change needed
        }
        
        List<Menu> allMenus = menuRepository.findAllByOrderByDisplayOrderAsc();
        
        // Remove the menu from its current position
        allMenus.removeIf(m -> m.getId().equals(menuId));
        
        // Insert at new position (adjust for 0-based index)
        int insertIndex = Math.max(0, Math.min(newPosition - 1, allMenus.size()));
        allMenus.add(insertIndex, menu);
        
        // Reassign all display orders
        for (int i = 0; i < allMenus.size(); i++) {
            allMenus.get(i).setDisplayOrder(i + 1);
        }
        
        menuRepository.saveAll(allMenus);
        log.info("Moved menu {} from position {} to position {}", menu.getName(), currentPosition, newPosition);
        
        return menu;
    }

    @Override
    public Menu insertMenuAtPosition(Menu menu, Integer position) {
        // First, increment orders of existing menus at and after the position
        menuRepository.incrementOrdersFrom(position);
        
        // Set the display order and save
        menu.setDisplayOrder(position);
        Menu savedMenu = menuRepository.save(menu);
        
        log.info("Inserted menu {} at position {}", menu.getName(), position);
        return savedMenu;
    }

    @Override
    public void reorderMenus(List<Long> menuIds) {
        for (int i = 0; i < menuIds.size(); i++) {
            Long menuId = menuIds.get(i);
            Menu menu = menuRepository.findById(menuId)
                    .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
            
            menu.setDisplayOrder(i + 1);
        }
        
        // Save all at once
        List<Menu> menusToUpdate = menuIds.stream()
                .map(id -> menuRepository.findById(id).orElseThrow())
                .toList();
        
        menuRepository.saveAll(menusToUpdate);
        log.info("Reordered {} menus based on provided sequence", menuIds.size());
    }

    @Override
    public void activateMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
        
        menu.setIsActive(true);
        menuRepository.save(menu);
        log.info("Activated menu: {}", menu.getName());
    }

    @Override
    public void deactivateMenu(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new IllegalArgumentException("Menu not found with id: " + menuId));
        
        menu.setIsActive(false);
        menuRepository.save(menu);
        log.info("Deactivated menu: {}", menu.getName());
    }
} 