package com.store.ecommerce.dto.wrapper;

import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.entity.ReportItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "ReportWrapper")
public class ReportWrapper extends ApiSuccessResponse<List<ReportItem>> {}
