package com.store.ecommerce.controller;

import com.store.ecommerce.dto.response.ApiErrorResponse;
import com.store.ecommerce.dto.response.ApiSuccessResponse;
import com.store.ecommerce.dto.wrapper.ReportWrapper;
import com.store.ecommerce.entity.ReportItem;
import com.store.ecommerce.enums.ReportBy;
import com.store.ecommerce.enums.ReportType;
import com.store.ecommerce.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "APIs for generating sales reports")
public class ReportController {
    private final ReportService reportService;

    // ===== REPORT BY PERIOD =====
    @Operation(
            summary = "Get report by period",
            description = """
                    Generate sales report by predefined period.
                    
                    Supported groupBy:
                    - sales-by-date
                    - sales-by-category
                    - sales-by-product
                    
                    Supported period:
                    - last-7-days
                    - last-28-days
                    - last-6-months
                    - last-year
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Report generated successfully",
                    content = @Content(schema = @Schema(implementation = ReportWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/period")
    public ResponseEntity<ApiSuccessResponse<List<ReportItem>>> getReportByPeriod(
            @RequestParam String groupBy,
            @RequestParam String period) {
        Date endTime = new Date();
        Date startTime;
        ReportType reportType = ReportType.DAY;

        switch (period) {
            case "last-7-days" -> startTime = calculateStartTime(7, reportType);
            case "last-28-days" -> startTime = calculateStartTime(28, reportType);
            case "last-6-months" -> {
                reportType = ReportType.MONTH;
                startTime = calculateStartTime(6, reportType);
            }
            case "last-year" -> {
                reportType = ReportType.MONTH;
                startTime = calculateStartTime(12, reportType);
            }
            default -> throw new IllegalArgumentException("Invalid period");
        }

        ReportBy reportBy = parseReportBy(groupBy);

        List<ReportItem> data = reportBy.equals(ReportBy.DATE)
                ? reportService.getReportSalesByDate(startTime, endTime, reportType)
                : reportService.getReportSalesByCategoryOrProduct(startTime, endTime, reportBy);

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<ReportItem>>builder()
                        .success(true)
                        .message("Report generated successfully")
                        .data(data)
                        .build()
        );
    }

    // ===== REPORT BY DATE RANGE =====
    @Operation(
            summary = "Get report by date range",
            description = "Generate sales report within a custom date range (max 30 days)"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Report generated successfully",
                    content = @Content(schema = @Schema(implementation = ReportWrapper.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid date range",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))
            )
    })
    @GetMapping("/range")
    public ResponseEntity<ApiSuccessResponse<List<ReportItem>>> getReportByDateRange(
            @RequestParam String groupBy,
            @RequestParam String startDate,
            @RequestParam String endDate) throws ParseException {

        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        Date start = formatter.parse(startDate);
        Date end = formatter.parse(endDate);

        validateDateRangeOrThrow(start, end);

        ReportBy reportBy = parseReportBy(groupBy);

        List<ReportItem> data = reportBy.equals(ReportBy.DATE)
                ? reportService.getReportSalesByDate(start, end, ReportType.DAY)
                : reportService.getReportSalesByCategoryOrProduct(start, end, reportBy);

        return ResponseEntity.ok(
                ApiSuccessResponse.<List<ReportItem>>builder()
                        .success(true)
                        .message("Report generated successfully")
                        .data(data)
                        .build()
        );
    }

    // ===== HELPER =====
    private ReportBy parseReportBy(String groupBy) {
        try {
            return switch (groupBy) {
                case "sales-by-date" -> ReportBy.DATE;
                case "sales-by-category" -> ReportBy.CATEGORY;
                case "sales-by-product" -> ReportBy.PRODUCT;
                default -> throw new IllegalArgumentException("Invalid groupBy value");
            };
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid groupBy value");
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

    private void validateDateRangeOrThrow(Date from, Date to) {
        if (to.before(from)) {
            throw new IllegalArgumentException("'endDate' must be after 'startDate'");
        }

        if (!to.before(new Date())) {
            throw new IllegalArgumentException("'endDate' must be before today");
        }

        long days = (to.getTime() - from.getTime()) / (1000 * 60 * 60 * 24);
        if (days >= 30) {
            throw new IllegalArgumentException("Date range must be less than 30 days");
        }
    }
}
