package com.store.ecommerce.service;

import com.store.ecommerce.entity.ReportItem;
import com.store.ecommerce.enums.ReportBy;
import com.store.ecommerce.enums.ReportType;

import java.util.Date;
import java.util.List;

public interface ReportService {
    List<ReportItem> getReportSalesByDate(Date startTime, Date endTime, ReportType reportType);

    List<ReportItem> getReportSalesByCategoryOrProduct(Date startTime, Date endTime, ReportBy reportBy);
}
