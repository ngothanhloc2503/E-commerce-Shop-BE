package com.store.ecommerce.service.impl;

import com.store.ecommerce.entity.Order;
import com.store.ecommerce.entity.OrderDetail;
import com.store.ecommerce.entity.ReportItem;
import com.store.ecommerce.enums.ReportBy;
import com.store.ecommerce.enums.ReportType;
import com.store.ecommerce.repository.OrderDetailRepository;
import com.store.ecommerce.repository.OrderRepository;
import com.store.ecommerce.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    private DateFormat dateFormatter;

    @Override
    public List<ReportItem> getReportSalesByDate(Date startTime, Date endTime, ReportType reportType) {
        List<Order> listOrders = orderRepository.findByOrderTimeBetween(startTime, endTime);
        dateFormatter = (reportType.equals(ReportType.DAY)) ? new SimpleDateFormat("yyyy-MM-dd")
                : new SimpleDateFormat("yyyy-MM");

        List<ReportItem> reportData = createReportData(startTime, endTime, reportType);

        for (Order order : listOrders) {
            String orderDateString = dateFormatter.format(order.getOrderTime());

            ReportItem reportItem = new ReportItem(orderDateString);

            int itemIndex = reportData.indexOf(reportItem);

            if (itemIndex >= 0) {
                reportItem = reportData.get(itemIndex);

                reportItem.addGrossSales(order.getTotal());
                reportItem.addNetSales(order.getSubtotal() - order.getProductCost());
                reportItem.increaseOrdersCount();
            }
        }

        return reportData;
    }

    private List<ReportItem> createReportData(Date startTime, Date endTime, ReportType reportType) {
        List<ReportItem> listReportItems = new ArrayList<>();

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(startTime);
        Calendar endDate = Calendar.getInstance();
        endDate.setTime(endTime);

        Date currentDate = startDate.getTime();
        String currentDateString = dateFormatter.format(currentDate);

        listReportItems.add(new ReportItem(currentDateString));

        do {
            if (reportType.equals(ReportType.DAY)) {
                startDate.add(Calendar.DAY_OF_MONTH, 1);
            } else if (reportType.equals(ReportType.MONTH)) {
                startDate.add(Calendar.MONTH, 1);
            }
            currentDate = startDate.getTime();
            currentDateString = dateFormatter.format(currentDate);

            listReportItems.add(new ReportItem(currentDateString));
        } while (startDate.before(endDate));

        return listReportItems;
    }

    @Override
    public List<ReportItem> getReportSalesByCategoryOrProduct(Date startTime, Date endTime, ReportBy reportBy) {
        List<OrderDetail> listOrderDetails = null;

        if (reportBy.equals(ReportBy.CATEGORY)) {
            listOrderDetails = orderDetailRepository.findWithCategoryAndTimeBetween(startTime, endTime);
        } else if (reportBy.equals(ReportBy.PRODUCT)) {
            listOrderDetails = orderDetailRepository.findWithProductAndTimeBetween(startTime, endTime);
        }

        List<ReportItem> listReportItems = new ArrayList<>();

        for (OrderDetail order : listOrderDetails) {
            String identifier = "";
            if (reportBy.equals(ReportBy.CATEGORY)) {
                identifier = order.getProduct().getCategory().getName();
            } else if (reportBy.equals(ReportBy.PRODUCT)) {
                identifier = order.getProduct().getName();
            }
            ReportItem item = new ReportItem(identifier);

            float grossSales = order.getSubtotal() + order.getShippingCost();
            float netSales = order.getSubtotal() - order.getProductCost();

            int itemIndex = listReportItems.indexOf(item);
            if (itemIndex >= 0) {
                item = listReportItems.get(itemIndex);

                item.addGrossSales(grossSales);
                item.addNetSales(netSales);
                item.increaseProductsCount(order.getQuantity());
            } else {
                listReportItems.add(new ReportItem(identifier, grossSales, netSales, order.getQuantity()));
            }
        }

        return listReportItems;
    }
}
