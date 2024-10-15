package com.store.ecommerce.util.exporter.product;

import com.store.ecommerce.dto.ProductDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.List;

public class ProductCsvExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<ProductDTO> listProducts) throws IOException {
        super.setResponseHeader(response, "text/csv", ".csv", "products_");

        String[] csvHeader = {"Product ID", "Name", "Brand", "Category"};
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader)
                .setSkipHeaderRecord(false)
                .build();

        CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), csvFormat);

        for (ProductDTO product : listProducts) {
            csvPrinter.printRecord(
                    product.getId(),
                    product.getName(),
                    product.getBrand().getName(),
                    product.getCategory().getName()
            );
        }
    }
}
