package com.ecommerce.inventory.service.mapper;

import com.ecommerce.inventory.domain.Product;
import com.ecommerce.inventory.service.dto.ProductDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Product} and its DTO {@link ProductDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProductMapper extends EntityMapper<ProductDTO, Product> {}
