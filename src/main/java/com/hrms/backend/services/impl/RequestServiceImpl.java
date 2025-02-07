package com.hrms.backend.services.impl;

import com.hrms.backend.dto.RequestDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.AttendanceRepository;
import com.hrms.backend.repository.RequestRepository;
import com.hrms.backend.repository.UserInfoRepository;
import com.hrms.backend.repository.UserRepository;
import com.hrms.backend.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final AttendanceRepository attendanceRepository;

    @Override
    public RequestDTO createRequest(RequestDTO requestDTO) {
        // Validate the request
        validateRequest(requestDTO);

        // Fetch the user from the database
        User user = userRepository.findById(Math.toIntExact(requestDTO.getUserId()))
                .orElseThrow(() -> new GenericException("User with ID " + requestDTO.getUserId() + " not found", HttpStatus.NOT_FOUND));

        // Create a new Request entity
        Request request = new Request();
        request.setUser(user);
        request.setRequestType(RequestType.valueOf(requestDTO.getRequestType()));
        request.setStartDate(requestDTO.getStartDate());
        request.setEndDate(requestDTO.getEndDate());
        request.setReason(requestDTO.getReason());
        request.setAllowanceAmount(requestDTO.getAllowanceAmount());
        request.setOvertimeHours(requestDTO.getOvertimeHours());
        request.setStatus(Status.PENDING);

        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_SICK_LEAVE.name()) || requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_SICK_LEAVE.name()) || requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_ANNUAL_LEAVE.name()) || requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_ANNUAL_LEAVE.name())) {
            // Calculate the number of leave days
            long daysBetween = ChronoUnit.DAYS.between(requestDTO.getStartDate(), requestDTO.getEndDate()) + 1;
            request.setLeaveDays((int) daysBetween);
        }

        // Save the request to the database
        Request savedRequest = requestRepository.save(request);

        // Return the saved request as DTO
        return mapToDTO(savedRequest);
    }

    private void validateRequest(RequestDTO requestDTO) {
        // Validation for Leave Requests (PAID_LEAVE and UNPAID_LEAVE)
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_SICK_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_SICK_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_ANNUAL_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_ANNUAL_LEAVE.name())) {
            if (requestDTO.getStartDate() == null || requestDTO.getEndDate() == null) {
                throw new GenericException("Start and End dates must be provided for leave requests.", HttpStatus.BAD_REQUEST);
            }
            if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())) {
                throw new GenericException("End date must be after Start date.", HttpStatus.BAD_REQUEST);
            }
            if (isUserPendingLeave(requestDTO.getUserId())) {
                throw new GenericException("User  cannot apply for another leave request while the current request is still PENDING.", HttpStatus.BAD_REQUEST);
            }
        }

        // Validation for Overtime Requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.OVERTIME.name())) {
            if (requestDTO.getOvertimeHours() <= 0 || requestDTO.getOvertimeHours() > 24) {
                throw new GenericException("Overtime hours must be provided and cannot exceed 24 hours.", HttpStatus.BAD_REQUEST);
            }

            // Validate that the overtime request date is not in the past
            if (requestDTO.getStartDate().isBefore(LocalDate.now())) {
                throw new GenericException("Overtime request date cannot be in the past.", HttpStatus.BAD_REQUEST);
            }

            // Check if the user is on leave on the same date
            if (isUserOnLeave(requestDTO.getUserId(), requestDTO.getStartDate())) {
                throw new GenericException("Overtime cannot be applied on days when the employee is on leave.", HttpStatus.BAD_REQUEST);
            }

            // Check if the user already has an overtime request on the same day
            if (isOvertimeRequestExists(requestDTO.getUserId(), requestDTO.getStartDate())) {
                throw new GenericException("User cannot apply for more than one overtime request on the same day.", HttpStatus.BAD_REQUEST);
            }
        }


        // Validation for Allowance Requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.ALLOWANCE.name())) {
            if (requestDTO.getAllowanceAmount() <= 0) {
                throw new GenericException("Allowance amount must be provided for an allowance request.", HttpStatus.BAD_REQUEST);
            }
        }
    }


    private boolean isUserPendingLeave(Long userId) {
        // Fetch the user from the database
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new GenericException("User not found", HttpStatus.BAD_REQUEST));


        List<Request> pendingLeaveRequests = new ArrayList<>();

        pendingLeaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.PAID_SICK_LEAVE, Status.PENDING));
        pendingLeaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.UNPAID_SICK_LEAVE, Status.PENDING));
        pendingLeaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.PAID_ANNUAL_LEAVE, Status.PENDING));
        pendingLeaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatus(user, RequestType.UNPAID_ANNUAL_LEAVE, Status.PENDING));

        return !pendingLeaveRequests.isEmpty();
    }


    public boolean isUserOnLeave(Long userId, LocalDate date) {
        // Fetch the user from the repository
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new GenericException("User not found", HttpStatus.BAD_REQUEST));


        List<Request> leaveRequests = new ArrayList<>();

        leaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatusAndStartDateBetween(
                user, RequestType.PAID_SICK_LEAVE, Status.APPROVED, date, date
        ));

        leaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatusAndStartDateBetween(
                user, RequestType.UNPAID_SICK_LEAVE, Status.APPROVED, date, date
        ));

        leaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatusAndStartDateBetween(
                user, RequestType.PAID_ANNUAL_LEAVE, Status.APPROVED, date, date
        ));

        leaveRequests.addAll(requestRepository.findByUserAndRequestTypeAndStatusAndStartDateBetween(
                user, RequestType.UNPAID_ANNUAL_LEAVE, Status.APPROVED, date, date
        ));

        return !leaveRequests.isEmpty();
    }


    private boolean isOvertimeRequestExists(Long userId, LocalDate date) {
        List<Request> overtimeRequests = requestRepository.findByUserAndRequestTypeAndStartDate(userRepository.findById(Math.toIntExact(userId))
                        .orElseThrow(() -> new GenericException("User not found", HttpStatus.BAD_REQUEST)),
                RequestType.OVERTIME, date);

        return !overtimeRequests.isEmpty();
    }


    private RequestDTO mapToDTO(Request request) {
        RequestDTO dto = new RequestDTO();
        dto.setId(request.getId());
        dto.setUserId(Long.valueOf(request.getUser().getId()));
        dto.setRequestType(request.getRequestType().toString());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setReason(request.getReason());
        dto.setAllowanceAmount(request.getAllowanceAmount());
        dto.setOvertimeHours(request.getOvertimeHours());
        dto.setStatus(request.getStatus().toString());
        return dto;

    }

    @Override
    public List<RequestDTO> getAllRequests() {
        List<Request> requests = requestRepository.findAll();
        return requests.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RequestDTO> getRequestById(Long requestId) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        return requestOpt.map(this::mapToDTO);
    }

    @Override
    public List<RequestDTO> getRequestsByUser(User user) {
        List<Request> requests = requestRepository.findByUser(user);
        return requests.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByStatus(Status status) {
        List<Request> requests = requestRepository.findByStatus(status);
        return requests.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<RequestDTO> getRequestsByCreatedDate(LocalDate date) {
        List<Request> requests = requestRepository.findByCreatedDate(date);
        return requests.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    @Override
    public Request updateRequestStatus(Long requestId, Status status) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);
        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();
            request.setStatus(status);
            return requestRepository.save(request);
        }
        return null;
    }

    @Override
    public RequestDTO approveRequest(Long requestId, int approverId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new GenericException("Leave request not found", HttpStatus.BAD_REQUEST));

        // Check if the request is already approved
        if (request.getStatus() == Status.APPROVED) {
            throw new GenericException("Leave request is already approved", HttpStatus.BAD_REQUEST);
        }

        // Update the request status and approved by
        if (request.getRequestType() == RequestType.PAID_SICK_LEAVE ||
                request.getRequestType() == RequestType.PAID_ANNUAL_LEAVE) {
            UserInfo userInfo = request.getUser().getUserInfo();

            if (request.getRequestType() == RequestType.PAID_SICK_LEAVE) {
                int newLeaveBalance = userInfo.getSickLeaveBalance() - request.getLeaveDays();
                userInfo.setSickLeaveBalance(newLeaveBalance);
            } else {
                int newLeaveBalance = userInfo.getAnnualLeaveBalance() - request.getLeaveDays();
                userInfo.setAnnualLeaveBalance(newLeaveBalance);
            }

        }
        request.setStatus(Status.APPROVED);

        request.setApprovedBy(userRepository.findById(approverId)
                .orElseThrow(() -> new GenericException("Approver not found", HttpStatus.BAD_REQUEST))
                .getId());

        // Save the updated request
        requestRepository.save(request);

        return mapToDTO(request);
    }


    @Override
    public RequestDTO rejectRequest(Long requestId, int rejecterId) {
        Optional<Request> requestOpt = requestRepository.findById(requestId);

        if (requestOpt.isPresent()) {
            Request request = requestOpt.get();

            request.setStatus(Status.REJECTED);

            // Set the ID of the user who rejected the request
            request.setRejectedBy(userRepository.findById(rejecterId)
                    .orElseThrow(() -> new GenericException("Rejecter not found", HttpStatus.BAD_REQUEST))
                    .getId());

            // Save the updated request
            requestRepository.save(request);

            return mapToDTO(request);
        }

        throw new GenericException("Leave request not found", HttpStatus.BAD_REQUEST);
    }


    private void updateAttendanceStatus(Request request) {
        // Assume that the leave request is for a specific date range
        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();

        // Check if the leave is approved as unpaid leave or not
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            AttendanceStatus status = request.getStatus() == Status.APPROVED ? AttendanceStatus.UNPAID_LEAVE : AttendanceStatus.valueOf(request.getRequestType().name());

            // Create or update attendance record for each day of the leave
            Attendance attendance = attendanceRepository.findByUserAndDate(request.getUser(), date)
                    .orElse(new Attendance(request.getUser(), date, status));

            attendance.setStatus(status);
            attendanceRepository.save(attendance);
        }
    }


}
