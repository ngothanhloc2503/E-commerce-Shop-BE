package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.OrderDetail;
import com.store.ecommerce.entity.ReportItem;
import com.store.ecommerce.enums.ReportBy;
import com.store.ecommerce.enums.ReportType;
import com.store.ecommerce.repository.OrderDetailRepository;
import com.store.ecommerce.repository.OrderRepository;
import com.store.ecommerce.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;

    @Override
    public List<ReportItem> getReportSalesByDate(Date startTime, Date endTime, ReportType reportType) {
        SimpleDateFormat dateFormatter = reportType.equals(ReportType.DAY)
                ? new SimpleDateFormat("yyyy-MM-dd")
                : new SimpleDateFormat("yyyy-MM");

        List<Order> listOrders = orderRepository.findByOrderTimeBetween(startTime, endTime);

        Map<ReportItem, ReportItem> reportMap = new LinkedHashMap<>();

        for (ReportItem item : createReportData(startTime, endTime, reportType)) {
            reportMap.put(item, item);
        }

        for (Order order : listOrders) {
            String orderDateString = dateFormatter.format(order.getOrderTime());
            ReportItem lookupKey = new ReportItem(orderDateString); // Dùng làm key để tìm kiếm

            ReportItem actualItem = reportMap.get(lookupKey);
            if (actualItem != null) {
                actualItem.addGrossSales(order.getTotal());
                actualItem.addNetSales(order.getSubtotal() - order.getProductCost());
                actualItem.increaseOrdersCount();
            }
        }

        return new ArrayList<>(reportMap.values());
    }

    private List<ReportItem> createReportData(Date startTime, Date endTime, ReportType reportType) {
        List<ReportItem> listReportItems = new ArrayList<>();
        SimpleDateFormat dateFormatter = reportType.equals(ReportType.DAY)
                ? new SimpleDateFormat("yyyy-MM-dd")
                : new SimpleDateFormat("yyyy-MM");

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(startTime);
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(endTime);

        listReportItems.add(new ReportItem(dateFormatter.format(startDate.getTime())));

        do {
            if (reportType.equals(ReportType.DAY)) {
                startDate.add(Calendar.DAY_OF_MONTH, 1);
            } else if (reportType.equals(ReportType.MONTH)) {
                startDate.add(Calendar.MONTH, 1);
            }
            listReportItems.add(new ReportItem(dateFormatter.format(startDate.getTime())));
        } while (startDate.before(endDate));

        return listReportItems;
    }

    @Override
    public List<ReportItem> getReportSalesByCategoryOrProduct(Date startTime, Date endTime, ReportBy reportBy) {
        List<OrderDetail> listOrderDetails;

        if (ReportBy.CATEGORY.equals(reportBy)) {
            listOrderDetails = orderDetailRepository.findWithCategoryAndTimeBetween(startTime, endTime);
        } else if (ReportBy.PRODUCT.equals(reportBy)) {
            listOrderDetails = orderDetailRepository.findWithProductAndTimeBetween(startTime, endTime);
        } else {
            throw new IllegalArgumentException("Unsupported ReportBy type: " + reportBy);
        }

        Map<ReportItem, ReportItem> reportMap = new LinkedHashMap<>();

        for (OrderDetail orderDetail : listOrderDetails) {
            String identifier = "";

            if (ReportBy.CATEGORY.equals(reportBy)) {
                identifier = (orderDetail.getProduct() != null && orderDetail.getProduct().getCategory() != null)
                        ? orderDetail.getProduct().getCategory().getName() : "Unknown Category";
            } else if (ReportBy.PRODUCT.equals(reportBy)) {
                identifier = (orderDetail.getProduct() != null)
                        ? orderDetail.getProduct().getName() : "Unknown Product";
            }

            float grossSales = orderDetail.getSubtotal() + orderDetail.getShippingCost();
            float netSales = orderDetail.getSubtotal() - orderDetail.getProductCost();

            ReportItem lookupKey = new ReportItem(identifier);
            ReportItem existingItem = reportMap.get(lookupKey);

            if (existingItem != null) {
                existingItem.addGrossSales(grossSales);
                existingItem.addNetSales(netSales);
                existingItem.increaseProductsCount(orderDetail.getQuantity());
            } else {
                ReportItem newItem = new ReportItem(identifier, grossSales, netSales, orderDetail.getQuantity());
                reportMap.put(newItem, newItem);
            }
        }

        return new ArrayList<>(reportMap.values());
    }
}