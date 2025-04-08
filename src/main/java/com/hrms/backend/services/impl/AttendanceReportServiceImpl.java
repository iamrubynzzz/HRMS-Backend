package com.hrms.backend.services.impl;

import com.hrms.backend.entities.Attendance;
import com.hrms.backend.services.AttendanceReportService;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AttendanceReportServiceImpl implements AttendanceReportService {
    private final DataSource dataSource;

    public String generateAttendanceReport(List<Attendance> attendanceRecords) throws JRException {
        // Load from classpath
        InputStream reportStream = getClass().getResourceAsStream("/attendanceReport.jrxml");
        if (reportStream == null) {
            throw new JRException("Could not find attendanceReport.jrxml in classpath.");
        }

        JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);

        // Create data source from attendance records
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(attendanceRecords);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("ReportTitle", "Attendance Report");

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

        String outputFilePath = "attendance_report.pdf";
        JasperExportManager.exportReportToPdfFile(jasperPrint, outputFilePath);

        System.out.println("Report saved at: " + new java.io.File(outputFilePath).getAbsolutePath());

        return outputFilePath;
    }

}
