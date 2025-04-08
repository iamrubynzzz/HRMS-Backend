package com.hrms.backend.services.impl;

import com.hrms.backend.entities.SalaryStatus;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.services.SalaryReportService;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.hrms.backend.entities.Salary;
import com.hrms.backend.repository.SalaryRepository;
import com.itextpdf.layout.property.UnitValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.nio.file.*;

@Service
public class SalaryReportServiceImpl implements SalaryReportService {

    @Autowired
    private SalaryRepository salaryRepository;

    public String generateSalaryReportById(Long salaryId) {
        Optional<Salary> salaryOpt = salaryRepository.findById(salaryId);

        if (salaryOpt.isEmpty()) {
            throw new GenericException("Salary not found for ID: " + salaryId, HttpStatus.NOT_FOUND);
        }

        Salary salary = salaryOpt.get();

        if (salary.getStatus() != SalaryStatus.RELEASED) {
            throw new GenericException("Salary has not been released yet.", HttpStatus.BAD_REQUEST);
        }

        return savePDF(salary);
    }

    private String savePDF(Salary salary) {
        try {
            // Get the Desktop path (assumes the user's desktop is available)
            String desktopPath = System.getProperty("user.home") + "/Desktop/";

            // Define the file path
            String fileName = "Salary_Report_" + salary.getUser().getName() + "_" + salary.getCalculationDate() + ".pdf";
            Path path = Paths.get(desktopPath + fileName);

            // Create PDF and write to the specified file
            PdfWriter writer = new PdfWriter(path.toFile());
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Adding content to the PDF document
            document.add(new Paragraph("Salary Report").setBold().setFontSize(16));
            document.add(new Paragraph("Generated on: " + java.time.LocalDate.now()).setItalic());

            Table table = new Table(new float[]{3, 3});
            table.setWidth(UnitValue.createPercentValue(100));

            table.addCell("Employee Name");
            table.addCell(salary.getUser().getName());

            table.addCell("Calculation Date");
            table.addCell(salary.getCalculationDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            table.addCell("Gross Salary");
            table.addCell(String.valueOf(salary.getGrossSalary()));

            table.addCell("Tax Deduction");
            table.addCell(String.valueOf(salary.getTaxDeduction()));

            table.addCell("Overtime Pay");
            table.addCell(String.valueOf(salary.getOvertimePayTotal()));

            table.addCell("Allowance");
            table.addCell(String.valueOf(salary.getAllowanceAmountTotal()));

            table.addCell("Net Salary");
            table.addCell(String.valueOf(salary.getNetSalary()));

            document.add(table);
            document.close();

            // Return the file path of the saved PDF
            return path.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error saving PDF report", e);
        }
    }
}
