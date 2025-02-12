package com.hrms.backend.services;

import com.hrms.backend.dto.RequestDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.Request;
import com.hrms.backend.entities.Status;
import com.hrms.backend.entities.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RequestService {
    RequestDTO createRequest(RequestDTO requestDTO);
    List<RequestDTO> getAllRequests();
    Optional<RequestDTO> getRequestById(Long requestId);
    List<RequestDTO> getRequestsByUser(User user);
    List<RequestDTO> getRequestsByStatus(Status status);
    List<RequestDTO> getRequestsByCreatedDate(LocalDate date);
    Request updateRequestStatus(Long requestId, Status status);

    RequestDTO approveRequest(Long requestId, int approverId);
    RequestDTO rejectRequest(Long requestId, int rejecterId);

}

