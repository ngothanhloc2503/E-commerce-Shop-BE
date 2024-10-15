package com.store.ecommerce.util.exporter.user;

import com.store.ecommerce.dto.UserDTO;
import com.store.ecommerce.util.exporter.AbstractExporter;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.util.List;

public class UserCsvExporter extends AbstractExporter {
    public void export(HttpServletResponse response, List<UserDTO> listUsers) throws IOException {
        super.setResponseHeader(response, "text/csv", ".csv", "users_");

        String[] csvHeader = {"User ID", "E-mail", "First Name", "Last Name", "Roles", "Enabled"};
        CSVFormat csvFormat = CSVFormat.Builder.create()
                .setHeader(csvHeader)
                .setSkipHeaderRecord(false)
                .build();

        CSVPrinter csvPrinter = new CSVPrinter(response.getWriter(), csvFormat);

        for (UserDTO user : listUsers) {
            csvPrinter.printRecord(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRoles(),
                    user.isEnabled()
            );
        }
    }
}
