package com.store.ecommerce.controller.staff;

import com.store.ecommerce.entity.ReportItem;
import com.store.ecommerce.enums.ReportBy;
import com.store.ecommerce.enums.ReportType;
import com.store.ecommerce.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController("SalesReportController")
@RequestMapping("/api/staff/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/{groupBy}/{period}")
    public ResponseEntity<List<ReportItem>> getReportSalesByDateWithPeriod(
            @PathVariable("groupBy") String groupBy,    // sales-by-date, sales-by-category or sales-by-product
            @PathVariable("period") String period) {
        Date endTime = new Date();
        Date startTime = null;
        ReportType reportType = ReportType.DAY;

        switch (period) {
            case "last-7-days":
                startTime = calculateStartTime(7, reportType);
                break;

            case "last-28-days":
                startTime = calculateStartTime(28, reportType);
                break;

            case "last-6-months":
                reportType = ReportType.MONTH;
                startTime = calculateStartTime(6, reportType);
                break;

            case "last-year":
                reportType = ReportType.MONTH;
                startTime = calculateStartTime(12, reportType);
                break;

            default:
                startTime = calculateStartTime(7, reportType);
                break;
        }

        ReportBy reportBy = ReportBy.valueOf(groupBy.substring(9).toUpperCase());
        if (reportBy.equals(ReportBy.DATE)) {
            return new ResponseEntity<>(
                    reportService.getReportSalesByDate(startTime, endTime, reportType),
                    HttpStatus.OK);
        } else {
            return new ResponseEntity<>(
                    reportService.getReportSalesByCategoryOrProduct(startTime, endTime, reportBy),
                    HttpStatus.OK);
        }
    }

    @GetMapping("/{groupBy}/{startTime}/{endTime}")
    public ResponseEntity<?> getReportSalesByDateWithDateRange(
            @PathVariable("groupBy") String groupBy,    // sales-by-date, sales-by-category or sales-by-product
            @PathVariable("startTime") String startTime,
            @PathVariable("endTime") String endTime) {
        ReportBy reportBy = ReportBy.valueOf(groupBy.substring(9).toUpperCase());
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

        try {
            Date startDate = dateFormatter.parse(startTime);
            Date endDate = dateFormatter.parse(endTime);

            Optional<String> result = validateDateRange(startDate, endDate);
            if (result.isPresent()) {
                return new ResponseEntity<>(result.get(), HttpStatus.BAD_REQUEST);
            }

            if (reportBy.equals(ReportBy.DATE)) {
                return new ResponseEntity<>(
                        reportService.getReportSalesByDate(startDate, endDate, ReportType.DAY),
                        HttpStatus.OK);
            } else {
                return new ResponseEntity<>(
                        reportService.getReportSalesByCategoryOrProduct(startDate, endDate, reportBy),
                        HttpStatus.OK);
            }
        } catch (ParseException e) {
            return new ResponseEntity<>("Could not parse start time and end time to date time.", HttpStatus.CONFLICT);
        }
    }

    private Date calculateStartTime(int num, ReportType reportType) {
        Calendar calendar = Calendar.getInstance();
        if (reportType.equals(ReportType.DAY)) {
            calendar.add(Calendar.DAY_OF_MONTH, -(num - 1));
        } else {
            calendar.add(Calendar.MONTH, -(num - 1));
        }

        return calendar.getTime();
    }

    private Optional<String> validateDateRange(Date fromDate, Date toDate) {
        Calendar calendar = Calendar.getInstance();

        // Check if toDate is after fromDate
        if (toDate.before(fromDate)) {
            return Optional.of("The 'To Date' must be after the 'From Date'.");
        }

        // Check if toDate is before the current date
        Date today = calendar.getTime();
        if (!toDate.before(today)) {
            return Optional.of("The 'To Date' must be before today's date.");
        }

        // Check if the date range is less than 30 days
        long millisecondsBetween = toDate.getTime() - fromDate.getTime();
        long daysBetween = millisecondsBetween / (1000 * 60 * 60 * 24); // Convert milliseconds to days
        if (daysBetween >= 30) {
            return Optional.of("The date range must be less than 30 days.");
        }

        return Optional.empty();
    }
}
