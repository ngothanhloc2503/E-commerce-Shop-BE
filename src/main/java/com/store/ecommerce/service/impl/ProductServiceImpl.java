package com.store.ecommerce.service.impl;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.dto.ProductImageDTO;
import com.store.ecommerce.entity.Product;
import com.store.ecommerce.entity.ProductDetail;
import com.store.ecommerce.entity.ProductImage;
import com.store.ecommerce.exception.ConflictException;
import com.store.ecommerce.exception.NotFoundException;
import com.store.ecommerce.mapper.ProductMapper;
import com.store.ecommerce.repository.ProductRepository;
import com.store.ecommerce.service.AWSS3Service;
import com.store.ecommerce.service.ProductService;
import com.store.ecommerce.util.PagingAndSortingHelper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.store.ecommerce.util.FileHelper.isFileNullOrEmpty;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final AWSS3Service awsS3Service;
    private final ProductMapper productMapper;

    // For Staff
    @Override
    public ProductDTO getProductByID(Long id) throws NotFoundException {
        Product product = productRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Could not find any product with ID: " + id));

        ProductDTO productDTO = productMapper.toProductDTO(product);
        setMainImagePath(productDTO);
        productDTO.getImages().forEach(this::setExtrasImagePath);
        return productDTO;
    }

    @Override
    public List<ProductDTO> getAllProducts(Long categoryID) {
        List<ProductDTO> all = null;
        if (categoryID < 1) {
            all = productRepository.findAll().stream().map(productMapper::toProductDTO).toList();
        } else {
            all = productRepository.findAllByCategory(categoryID).stream().map(productMapper::toProductDTO).toList();
        }
        all.forEach(this::setMainImagePath);
        all.forEach(product -> {
            product.getImages().forEach(this::setExtrasImagePath);
        });
        return all;
    }

    @Override
    public List<ProductDTO> getAllProducts(Long categoryID, String keyword, String sortField, String sortDir) {
        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

        List<ProductDTO> all = null;
        if (categoryID < 1) {
            all = productRepository.findAll(keyword, sort)
                    .stream().map(productMapper::toProductDTO).toList();
        } else {
            all = productRepository.findAllByCategory(categoryID, keyword, sort)
                    .stream().map(productMapper::toProductDTO).toList();
        }
        all.forEach(this::setMainImagePath);
        all.forEach(product -> {
            product.getImages().forEach(this::setExtrasImagePath);
        });
        return all;
    }

    @Override
    public Page<ProductDTO> getProductByPage(PagingAndSortingHelper helper, Long categoryID) {
        Page<Product> pageProduct = null;
        if (categoryID < 1) {
            pageProduct = (Page<Product>) helper.getPageEntities(productRepository);
        } else {
            Sort sort = Sort.by(helper.getSortField());
            sort = helper.getSortDir().equalsIgnoreCase("asc") ? sort.ascending() : sort.descending();

            Pageable pageable = PageRequest.of(helper.getPageNum() - 1, helper.getPageSize(), sort);

            if (helper.getKeyword() != null) {
                pageProduct = productRepository.searchByCategory(categoryID, helper.getKeyword(), pageable);
            } else {
                pageProduct = productRepository.findAllByCategory(categoryID, pageable);
            }
        }
        Page<ProductDTO> pageProductDTO = pageProduct.map(productMapper::toProductDTO);
        pageProductDTO.map(this::setMainImagePath);
        pageProductDTO.forEach(productDTO -> {
            productDTO.getImages().forEach(this::setExtrasImagePath);
        });
        return pageProductDTO;
    }

    @Override
    public void changeEnabledStatus(Long id, boolean status) throws NotFoundException {
        if (productRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any product with ID: " + id);
        }

        productRepository.updateEnabledStatus(id, status);
    }

    private void deleteExtraImagesWereRemovedOnForm(ProductDTO productDTO) {
        String extraImageDir = "product-images/" + productDTO.getId() + "/extras/";
        List<String> listObjectKeys = awsS3Service.listFolder(extraImageDir);

        for (String objectKey : listObjectKeys) {
            int lastIndexOfSlash = objectKey.lastIndexOf("/");
            String fileName = objectKey.substring(lastIndexOfSlash + 1, objectKey.length());

            if (!productDTO.containsImageName(fileName)) {
                awsS3Service.deleteFile(objectKey);
            }
        }
    }

    private void saveUploadImages(MultipartFile mainImageMultipart, MultipartFile[] listExtrasImageFile,
                                  ProductDTO savedProduct) throws IOException {
        if (!isFileNullOrEmpty(mainImageMultipart)) {
            String fileName = StringUtils.cleanPath(mainImageMultipart.getOriginalFilename());
            String uploadDir = "product-images/" + savedProduct.getId();

            List<String> listObjectKeys = awsS3Service.listFolder(uploadDir + "/");
            for (String objectKey : listObjectKeys) {
                if (objectKey.startsWith(uploadDir + "/") && !objectKey.contains("/extras/")) {
                    awsS3Service.deleteFile(objectKey);
                }
            }

            awsS3Service.uploadFile(uploadDir, fileName,
                    mainImageMultipart.getInputStream(),
                    mainImageMultipart.getSize(),
                    mainImageMultipart.getContentType());
        }

        if (listExtrasImageFile == null) return;

        if (listExtrasImageFile.length > 0) {
            String uploadDir = "product-images/" + savedProduct.getId() + "/extras";

            for (MultipartFile extrasImageFile : listExtrasImageFile) {
                if (isFileNullOrEmpty(extrasImageFile)) continue;

                String fileName = StringUtils.cleanPath(extrasImageFile.getOriginalFilename());
                awsS3Service.uploadFile(uploadDir, fileName,
                        extrasImageFile.getInputStream(),
                        extrasImageFile.getSize(),
                        extrasImageFile.getContentType());
            }
        }
    }

    private void setMainImageName(ProductDTO productDTO, MultipartFile mainImageMultipart) {
        if (!isFileNullOrEmpty(mainImageMultipart)) {
            String fileName = StringUtils.cleanPath(Objects.requireNonNull(mainImageMultipart.getOriginalFilename()));
            productDTO.setMainImage(fileName);
        } else {
            if (!StringUtils.hasText(productDTO.getMainImage())) productDTO.setMainImage(null);
        }
    }

    @Override
    public ProductDTO saveProduct(ProductDTO productDTO,
                                  MultipartFile mainImageFile,
                                  MultipartFile[] extrasImagesFile
    ) throws ConflictException, IOException {
        if (!isNameUnique(productDTO.getId(), productDTO.getName())) {
            throw new ConflictException("Product name already exists!");
        }

        setMainImageName(productDTO, mainImageFile);
        Product product = productMapper.toProduct(productDTO);

        boolean isUpdating = (product.getId() != null && product.getId() > 0);
        if (!isUpdating) {
            setExtrasImage(product);
            setDetail(product);
            product.setCreatedTime(new Date());
        } else {
            Product productInDB = productRepository.findById(product.getId()).orElseThrow();
            if (product.getMainImage() == null) {
                product.setMainImage(productInDB.getMainImage());
            }

            setExtrasImage(product);
            setDetail(product);

            product.setUpdatedTime(new Date());
        }

        product.setAlias();
        Product saved = productRepository.save(product);


        ProductDTO savedProductDto = productMapper.toProductDTO(saved);

        saveUploadImages(mainImageFile, extrasImagesFile, savedProductDto);
        deleteExtraImagesWereRemovedOnForm(savedProductDto);

        return savedProductDto;
    }

    @Override
    public boolean isNameUnique(Long id, String name) {
        Optional<Product> product = productRepository.findByName(name);
        return product.isEmpty() || Objects.equals(product.get().getId(), id);
    }

    @Override
    public void deleteProduct(Long id) throws NotFoundException {
        if (productRepository.findById(id).isEmpty()) {
            throw new NotFoundException("Could not find any product with ID: " + id);
        }

        productRepository.deleteById(id);
    }

    private void setDetail(Product product) {
        for (ProductDetail detail : product.getDetails()) {
            if (detail.getId() != null) {
                detail.setId(null);
            }
            detail.setProduct(product);
        }
    }

    private void setExtrasImage(Product product) {
        for (ProductImage image : product.getImages()) {
            if (image.getId() != null) {
                image.setId(null);
            }
            image.setProduct(product);
        }
    }

    private void setExtrasImagePath(ProductImageDTO productImageDTO) {
        String dir = "product-images/" + productImageDTO.getProductID() + "/extras";
        productImageDTO.setImagePath(awsS3Service.getImagePath(dir, productImageDTO.getName()));
    }

    private ProductDTO setMainImagePath(ProductDTO productDTO) {
        String dir = "product-images/" + productDTO.getId();
        productDTO.setMainImagePath(awsS3Service.getImagePath(dir, productDTO.getMainImage()));
        return productDTO;
    }

    // For Customer
    @Override
    public List<ProductDTO> getProductForHomePage() {
        Sort sort = Sort.by("averageRating");
        sort = sort.descending();

        List<Product> all = productRepository.findAll(sort);

        List<ProductDTO> topFifteenRatedProduct = new ArrayList<>();
        int size = Math.min(all.size(), 15);
        for (int i = 0; i < size; i++) {
            topFifteenRatedProduct.add(productMapper.toProductDTO(all.get(i)));
        }
        topFifteenRatedProduct.forEach(this::setMainImagePath);
        topFifteenRatedProduct.forEach(product -> {
            product.getImages().forEach(this::setExtrasImagePath);
        });
        return topFifteenRatedProduct;
    }

    @Override
    public ProductDTO getProductByAlias(String alias) throws NotFoundException {
        Product productByAlias = productRepository.findByAlias(alias).orElseThrow(
                () -> new NotFoundException("Product isn't existing!"));

        ProductDTO productDTO = productMapper.toProductDTO(productByAlias);
        setMainImagePath(productDTO);
        productDTO.getImages().forEach(this::setExtrasImagePath);
        return productDTO;
    }

    @Override
    public Page<ProductDTO> getProductByCategoryName(String categoryName, int pageNum) {
        List<Product> list = productRepository.findAllEnabled();
        if (!categoryName.isEmpty() && !categoryName.isBlank()) {
            list = list.stream().filter(p -> p.getCategory().getAllParentName()
                    .contains(categoryName)).collect(Collectors.toList());
        }

        // Convert to page
        Pageable pageable = PageRequest.of(pageNum - 1, 15);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<Product> pageContent = list.subList(start, end);
        Page<Product> page = new PageImpl<>(pageContent, pageable, list.size());

        return page.map(productMapper::toProductDTO)
                .map(this::setMainImagePath);
    }

    @Override
    public Page<ProductDTO> searchProduct(String keyword, int pageNum, String sortField,
                                          Float rating, Long[] brandIDs) {
        int underScorePosition = sortField.indexOf("_");
        String sortDir = sortField.substring(underScorePosition + 1);
        sortField = sortField.substring(0, underScorePosition);

        Sort sort = Sort.by(sortField);
        sort = sortDir.equalsIgnoreCase("asc") ? sort.ascending() :sort.descending();
        Pageable pageable = PageRequest.of(pageNum - 1, 15, sort);

        List<Long> brandIdList = (brandIDs == null || brandIDs.length == 0)
                ? null
                : Arrays.asList(brandIDs);

        Page<Product> searchResult = productRepository.searchProduct(keyword, rating, brandIdList, pageable);

        return searchResult.map(productMapper::toProductDTO)
                .map(this::setMainImagePath);
    }
}
