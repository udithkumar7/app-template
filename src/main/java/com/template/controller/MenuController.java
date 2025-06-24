package com.template.controller;

import com.template.dto.MenuCreateRequest;
import com.template.entity.Menu;
import com.template.entity.Role;
import com.template.entity.RoleMenu;
import com.template.repository.MenuRepository;
import com.template.repository.RoleMenuRepository;
import com.template.repository.RoleRepository;
import com.template.service.MenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
@Validated
@Slf4j
public class MenuController {
    private final MenuRepository menuRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuService menuService;

    // Create a menu (superadmin only)
    @PostMapping
    public ResponseEntity<?> createMenu(@Valid @RequestBody MenuCreateRequest request) {
        try {
            log.info("Creating menu with request: name={}, parentMenuId={}", 
                    request.getName(), request.getParentMenuId());
            
            Menu menu = convertToMenu(request);
            log.info("Converted menu object: name={}, parentMenu={}", 
                    menu.getName(), menu.getParentMenu() != null ? menu.getParentMenu().getId() : "null");
            
            Menu createdMenu = menuService.createMenu(menu);
            
            // Verify the saved menu with parent fetched
            Menu verifiedMenu = menuRepository.findByIdWithParent(createdMenu.getId()).orElse(null);
            if (verifiedMenu != null && verifiedMenu.getParentMenu() != null) {
                log.info("Menu saved successfully with parent_menu_id: {}", verifiedMenu.getParentMenu().getId());
            } else if (verifiedMenu != null) {
                log.info("Menu saved successfully with no parent menu");
            } else {
                log.error("Failed to verify saved menu");
            }
            
            return ResponseEntity.ok(createdMenu);
        } catch (IllegalArgumentException e) {
            log.error("Error creating menu: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private Menu convertToMenu(MenuCreateRequest request) {
        Menu.MenuBuilder menuBuilder = Menu.builder()
                .name(request.getName())
                .path(request.getPath())
                .displayOrder(request.getDisplayOrder())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .icon(request.getIcon());
        
        // Set parent menu if provided
        if (request.getParentMenuId() != null) {
            Menu parentMenu = menuRepository.findById(request.getParentMenuId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent menu not found with id: " + request.getParentMenuId()));
            menuBuilder.parentMenu(parentMenu);
            log.info("Setting parent menu {} for new menu {}", parentMenu.getName(), request.getName());
        }
        
        return menuBuilder.build();
    }

    // List all menus (ordered by display order)
    @GetMapping
    public List<Menu> getAllMenus() {
        return menuService.getAllMenusOrdered();
    }

    // Get active menus only (ordered)
    @GetMapping("/active")
    public List<Menu> getActiveMenus() {
        return menuService.getActiveMenusOrdered();
    }

    // Get root menus only (no parent)
    @GetMapping("/root")
    public List<Menu> getRootMenus() {
        return menuService.getRootMenusOrdered();
    }

    // Get sub-menus for a parent menu
    @GetMapping("/{parentId}/submenu")
    public List<Menu> getSubMenus(@PathVariable @Positive(message = "Parent ID must be positive") Long parentId) {
        return menuService.getSubMenusOrdered(parentId);
    }

    // Get menu by ID
    @GetMapping("/{id}")
    public ResponseEntity<Menu> getMenu(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        return menuService.getMenuById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    // Debug endpoint - Get menu with parent details
    @GetMapping("/{id}/debug")
    public ResponseEntity<Map<String, Object>> getMenuDebug(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        Optional<Menu> menuOpt = menuRepository.findByIdWithParent(id);
        if (menuOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Menu menu = menuOpt.get();
        Map<String, Object> debugInfo = Map.of(
            "id", menu.getId(),
            "name", menu.getName(),
            "path", menu.getPath(),
            "displayOrder", menu.getDisplayOrder(),
            "parentMenuId", menu.getParentMenu() != null ? menu.getParentMenu().getId() : null,
            "parentMenuName", menu.getParentMenu() != null ? menu.getParentMenu().getName() : null,
            "hasParent", menu.getParentMenu() != null
        );
        
        return ResponseEntity.ok(debugInfo);
    }

    // Update menu
    @PutMapping("/{id}")
    public ResponseEntity<Menu> updateMenu(@PathVariable @Positive(message = "Menu ID must be positive") Long id, @Valid @RequestBody Menu menu) {
        try {
            return ResponseEntity.ok(menuService.updateMenu(id, menu));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete menu
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMenu(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        try {
            menuService.deleteMenu(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // === Menu Ordering Endpoints ===

    // Move menu up by one position
    @PutMapping("/{id}/move-up")
    public ResponseEntity<Menu> moveMenuUp(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        try {
            return ResponseEntity.ok(menuService.moveMenuUp(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Move menu down by one position
    @PutMapping("/{id}/move-down")
    public ResponseEntity<Menu> moveMenuDown(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        try {
            return ResponseEntity.ok(menuService.moveMenuDown(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Move menu to specific position
    @PutMapping("/{id}/move-to/{position}")
    public ResponseEntity<?> moveMenuToPosition(@PathVariable @Positive(message = "Menu ID must be positive") Long id, @PathVariable @Positive(message = "Position must be positive") Integer position) {
        try {
            return ResponseEntity.ok(menuService.moveMenuToPosition(id, position));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Bulk reorder menus
    @PutMapping("/reorder")
    public ResponseEntity<String> reorderMenus(@RequestBody @NotEmpty(message = "Menu IDs list cannot be empty") List<@Positive(message = "Each menu ID must be positive") Long> menuIds) {
        try {
            menuService.reorderMenus(menuIds);
            return ResponseEntity.ok("Menus reordered successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid menu IDs provided");
        }
    }

    // Activate/Deactivate menu
    @PutMapping("/{id}/activate")
    public ResponseEntity<String> activateMenu(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        try {
            menuService.activateMenu(id);
            return ResponseEntity.ok("Menu activated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateMenu(@PathVariable @Positive(message = "Menu ID must be positive") Long id) {
        try {
            menuService.deactivateMenu(id);
            return ResponseEntity.ok("Menu deactivated");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Set menus for a role (superadmin only)
    @PostMapping("/role/{roleId}")
    public ResponseEntity<?> setMenusForRole(
            @PathVariable @Positive(message = "Role ID must be positive") Long roleId,
            @RequestBody @NotEmpty(message = "Menu IDs set cannot be empty") Set<@Positive(message = "Each menu ID must be positive") Long> menuIds) {
        try {
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role not found with id: " + roleId));
            }
            Role role = roleOpt.get();
            
            // Validate that all menu IDs exist
            List<Menu> menus = menuRepository.findAllById(menuIds);
            if (menus.size() != menuIds.size()) {
                return ResponseEntity.badRequest().body(Map.of("error", "One or more menu IDs not found"));
            }
            
            // Remove existing mappings
            List<RoleMenu> existing = roleMenuRepository.findByRole(role);
            roleMenuRepository.deleteAll(existing);
            
            // Add new mappings
            List<RoleMenu> newMappings = menus.stream()
                    .map(menu -> RoleMenu.builder().role(role).menu(menu).build())
                    .collect(Collectors.toList());
            roleMenuRepository.saveAll(newMappings);
            
            log.info("Set {} menus for role: {}", menus.size(), role.getName());
            return ResponseEntity.ok(Map.of("message", "Menus set for role successfully", "count", menus.size()));
        } catch (Exception e) {
            log.error("Error setting menus for role: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to set menus for role"));
        }
    }

    // Get menus for a role
    @GetMapping("/role/{roleId}")
    public ResponseEntity<?> getMenusForRole(@PathVariable @Positive(message = "Role ID must be positive") Long roleId) {
        try {
            Optional<Role> roleOpt = roleRepository.findById(roleId);
            if (roleOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role not found with id: " + roleId));
            }
            Role role = roleOpt.get();
            List<RoleMenu> roleMenus = roleMenuRepository.findByRole(role);
            List<Menu> menus = roleMenus.stream()
                    .map(RoleMenu::getMenu)
                    .sorted((m1, m2) -> m1.getDisplayOrder().compareTo(m2.getDisplayOrder()))
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                    "role", role.getName(),
                    "menus", menus,
                    "count", menus.size()
            ));
        } catch (Exception e) {
            log.error("Error getting menus for role: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get menus for role"));
        }
    }
} 