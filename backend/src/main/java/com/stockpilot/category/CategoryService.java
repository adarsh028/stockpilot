package com.stockpilot.category;

import com.stockpilot.category.dto.CategoryRequest;
import com.stockpilot.category.dto.CategoryResponse;
import com.stockpilot.common.exception.ConflictException;
import com.stockpilot.tenant.CurrentTenant;
import com.stockpilot.tenant.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CurrentTenant currentTenant;
    private final TenantGuard tenantGuard;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return categoryRepository.findByOrganizationIdOrderByName(currentTenant.organizationId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest req) {
        UUID orgId = currentTenant.organizationId();
        if (categoryRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, req.name())) {
            throw new ConflictException("A category with this name already exists: " + req.name());
        }
        Category category = new Category();
        category.setOrganizationId(orgId);
        category.setName(req.name());
        category.setActive(req.isActive() == null || req.isActive());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest req) {
        UUID orgId = currentTenant.organizationId();
        Category category = tenantGuard.loadOwned("Category", id, orgId,
                () -> categoryRepository.findByIdAndOrganizationId(id, orgId));

        if (!category.getName().equalsIgnoreCase(req.name())
                && categoryRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, req.name())) {
            throw new ConflictException("A category with this name already exists: " + req.name());
        }
        category.setName(req.name());
        if (req.isActive() != null) {
            category.setActive(req.isActive());
        }
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deactivate(UUID id) {
        UUID orgId = currentTenant.organizationId();
        Category category = tenantGuard.loadOwned("Category", id, orgId,
                () -> categoryRepository.findByIdAndOrganizationId(id, orgId));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId().toString(),
                category.getName(),
                category.isActive()
        );
    }
}
