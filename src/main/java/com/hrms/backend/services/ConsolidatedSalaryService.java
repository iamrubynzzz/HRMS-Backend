package com.hrms.backend.services;

import com.hrms.backend.dto.ConsolidatedSalaryDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ConsolidatedSalaryService {;
 //   List<ConsolidatedSalaryDTO> getConsolidatedSalaries();
    Page<ConsolidatedSalaryDTO> getFilteredConsolidatedSalaries(LocalDate startDate, LocalDate endDate, int page, int size);
}
