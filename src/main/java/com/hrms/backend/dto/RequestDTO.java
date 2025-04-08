package com.hrms.backend.dto;

import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.RequestType;
import com.hrms.backend.entities.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class RequestDTO {
    private Long id;
    private Long userId;
    private String requestType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer leaveDays;
    private String reason;
    private String status;
    private Float allowanceAmount;
    private Integer overtimeHours;
    private String employeeName;

    public RequestDTO(Request savedRequest) {
        this.id = savedRequest.getId();
        this.userId = Long.valueOf(savedRequest.getUser().getId());
        this.requestType = savedRequest.getRequestType().name();
        this.startDate = savedRequest.getStartDate();
        this.endDate = savedRequest.getEndDate();
        this.leaveDays = savedRequest.getLeaveDays();
        this.reason = savedRequest.getReason();
        this.status = savedRequest.getStatus().name();
        this.allowanceAmount = savedRequest.getAllowanceAmount();
        this.overtimeHours = savedRequest.getOvertimeHours();
        this.employeeName = savedRequest.getUser().getName();
    }
}
