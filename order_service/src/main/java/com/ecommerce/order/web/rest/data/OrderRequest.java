package com.ecommerce.order.web.rest.data;

import com.ecommerce.order.service.dto.OrderDTO;
import com.ecommerce.order.service.dto.OrderItemDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    private List<OrderItemDTO> orderItems;

}
