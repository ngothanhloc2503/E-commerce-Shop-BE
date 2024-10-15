package com.store.ecommerce.util.exporter.category;

import com.store.ecommerce.dto.CategoryDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.List;

public class CategoryCsvExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<CategoryDTO> listCategories) throws IOException {
        super.setResponseHeader(response, "text/csv", ".csv", "categories_");

        String[] csvHeader = {"Category ID", "Category Name"};
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader)
                .setSkipHeaderRecord(false)
                .build();

        CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), csvFormat);

        for (CategoryDTO cat : listCategories) {
            csvPrinter.printRecord(
                    cat.getId(),
                    cat.getName()
            );
        }
    }
}
