package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.Address;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddressWrapper")
public class AddressWrapper extends ApiSuccessResponse<Address> {
}
