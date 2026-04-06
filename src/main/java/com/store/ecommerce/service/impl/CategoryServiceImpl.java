package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.entity.Category;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.CategoryMapper;
import com.store.ecommerce.repository.CategoryRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.CategoryService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;
    private final CategoryRepository categoryRepository;
    private final AWSS3Service awsS3Service;

    // For Staff
    @Override
    public List<CategoryDTO> getAllCategories() {
        List<CategoryDTO> categories = categoryRepository.findAll().stream().map(categoryMapper::toCategoryDTO).toList();
        categories.forEach(this::setImagePathForCategory);
        return categories;
    }

    @Override
    public List<CategoryDTO> getAllCategories(String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<CategoryDTO> categories = categoryRepository.findAll(keyword, sort)
                .stream().map(categoryMapper::toCategoryDTO).toList();
        categories.forEach(this::setImagePathForCategory);
        return categories;
    }

    @Override
    public Page<CategoryDTO> getCategoriesByPage(PagingAndSortingHelper helper) {
        Page<Category> pageCategory = (Page<Category>) helper.getPageEntities(categoryRepository);
        Page<CategoryDTO> pageCategoriesDTO = pageCategory.map(categoryMapper::toCategoryDTO);
        pageCategoriesDTO.map(this::setImagePathForCategory);
        return pageCategoriesDTO;
    }

    @Override
    public CategoryDTO getCategoryById(Long id) throws NotFoundException {
        Category category = categoryRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any category with ID: " + id));

        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(category);
        setImagePathForCategory(categoryDTO);
        return categoryDTO;
    }

    @Override
    public CategoryDTO save(CategoryDTO categoryDTO, MultipartFile image) throws ConflictException, NotFoundException, IOException {
        if (!isNameUnique(categoryDTO.getId(), categoryDTO.getName())) {
            throw new ConflictException("Category name already exists!");
        }

        // Handle image
        if (!isFileNullOrEmpty(image)) {
            String originalName = image.getOriginalFilename();
            String fileName = UUID.randomUUID() + "_" + originalName;
            categoryDTO.setImage(fileName);
        } else if (StringUtils.hasText(categoryDTO.getImage())) {
            categoryDTO.setImage(null);
        }

        // Save category
        boolean isUpdating = (categoryDTO.getId() != null);
        if (isUpdating) {
            if (categoryDTO.getImage() == null) {
                Category saved = categoryRepository.findById(categoryDTO.getId()).orElseThrow(
                        () -> new NotFoundException("Category not found")
                );
                categoryDTO.setImage(saved.getImage());
            }
        }

        Category category = categoryMapper.toCategory(categoryDTO);
        if (categoryDTO.getParentID() > 0) {
            Category parent = categoryRepository.findById(categoryDTO.getParentID()).orElseThrow(
                    () -> new NotFoundException("Category parent is not exist!"));

            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        CategoryDTO savedCategory = categoryMapper.toCategoryDTO(categoryRepository.save(category));

        // Upload image if exists
        if (!isFileNullOrEmpty(image)) {
            String uploadDir = "category-images/" + savedCategory.getId();

            awsS3Service.removeFolder(uploadDir + "/");
            awsS3Service.uploadFile(uploadDir, categoryDTO.getImage(),
                    image.getInputStream(), image.getSize(), image.getContentType());
        }

        return setImagePathForCategory(savedCategory);
    }

    @Override
    public boolean isNameUnique(Long id, String name) {
        Optional<Category> category = categoryRepository.findByName(name);
        return category.isEmpty() || Objects.equals(category.get().getId(), id);
    }

    @Override
    public void updateCategoryEnabledStatus(Long id, boolean status) throws NotFoundException {
        if(categoryRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any category with ID: " + id);
        }

        categoryRepository.updateEnabledStatus(id, status);
    }

    @Override
    public void delete(Long id) throws NotFoundException {
        Category savedCategory = categoryRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any category with ID: " + id));

        List<Category> listChildren = savedCategory.getChildren().stream().toList();
        for (Category child : listChildren) {
            child.setParent(null);
            categoryRepository.save(child);
        }

        categoryRepository.deleteById(id);

        String dir = "category-images/" + id;
        awsS3Service.removeFolder(dir + "/");
    }

    private CategoryDTO setImagePathForCategory(CategoryDTO categoryDTO) {
        categoryDTO.setImagePath(awsS3Service.getImagePath("category-images/" + categoryDTO.getId(), categoryDTO.getImage()));
        return categoryDTO;
    }

    // For Customer
    @Override
    public List<CategoryDTO> getAllCategoriesEnabled() {
        List<CategoryDTO> all = categoryRepository.getAllCategoriesEnabled().stream().map(categoryMapper::toCategoryDTO).toList();
        all.forEach(this::setImagePathForCategory);
        return all;
    }

    @Override
    public CategoryDTO getCategoryByName(String name) throws NotFoundException {
        Category categoryByName = categoryRepository.getCategoryByName(name).orElseThrow(
                () ->new NotFoundException("Category isn't existing!"));

        CategoryDTO categoryDTO = categoryMapper.toCategoryDTO(categoryByName);
        setImagePathForCategory(categoryDTO);

        Queue<CategoryDTO> queue = new LinkedList<>(categoryDTO.getChildren());
        Set<CategoryDTO> children = new HashSet<>();
        while (!queue.isEmpty()) {
            CategoryDTO temp = queue.poll();
            queue.addAll(temp.getChildren());
            children.add(temp);
        }
        categoryDTO.setChildren(children);

        for (CategoryDTO cat : categoryDTO.getChildren()) {
            setImagePathForCategory(cat);
            cat.setChildren(null);
        }
        return categoryDTO;
    }
}
