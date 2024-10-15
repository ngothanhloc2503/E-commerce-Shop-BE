package com.store.ecommerce.util.exporter.brand;

import com.store.ecommerce.dto.BrandDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.List;

public class BrandCsvExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<BrandDTO> listBrands) throws IOException {
        super.setResponseHeader(response, "text/csv", ".csv", "brands_");

        String[] csvHeader = {"Brand ID", "Brand Name"};
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader)
                .setSkipHeaderRecord(false)
                .build();

        CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), csvFormat);

        for (BrandDTO brand : listBrands) {
            csvPrinter.printRecord(
                    brand.getId(),
                    brand.getName()
            );
        }
    }
}
