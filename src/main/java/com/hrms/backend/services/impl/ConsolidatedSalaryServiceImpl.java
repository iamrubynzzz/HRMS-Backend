package com.hrms.backend.services.impl;

import com.hrms.backend.dto.ConsolidatedSalaryDTO;
import com.hrms.backend.repository.ConsolidatedSalaryRepository;
import com.hrms.backend.services.ConsolidatedSalaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ConsolidatedSalaryServiceImpl implements ConsolidatedSalaryService {
    private final ConsolidatedSalaryRepository consolidatedSalaryRepository;

    public Page<ConsolidatedSalaryDTO> getFilteredConsolidatedSalaries(LocalDate startDate, LocalDate endDate, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("calculationDate").descending());
        return consolidatedSalaryRepository.findFilteredConsolidatedSalaries(startDate, endDate, pageable);
    }

}
