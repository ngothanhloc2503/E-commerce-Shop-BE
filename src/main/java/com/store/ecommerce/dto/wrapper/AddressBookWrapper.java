package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.AddressBookResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddressBookWrapper")
public class AddressBookWrapper extends ApiSuccessResponse<AddressBookResponse> {}
