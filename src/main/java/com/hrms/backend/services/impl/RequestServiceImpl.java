package com.hrms.backend.services.impl;

import com.hrms.backend.dto.NotificationMessage;
import com.hrms.backend.dto.RequestDTO;
import com.hrms.backend.entities.*;
import com.hrms.backend.exception.AccessDeniedException;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.*;
import com.hrms.backend.services.NotificationService;
import com.hrms.backend.services.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor

public class RequestServiceImpl implements RequestService {
    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Override
    public RequestDTO createRequest(RequestDTO requestDTO) {
        // Validate the request
        validateRequest(requestDTO);

        // Check if userId is null
        if (requestDTO.getUserId() == null) {
            throw new GenericException("User ID must be provided.", HttpStatus.BAD_REQUEST);
        }

        // Parse userId to Long
        Long userId = Long.parseLong(String.valueOf(requestDTO.getUserId()));

        // Fetch the user from the database
        User user = userRepository.findById(Math.toIntExact(userId))
                .orElseThrow(() -> new GenericException("User with ID " + userId + " not found", HttpStatus.NOT_FOUND));

        // Create a new Request entity
        Request request = new Request();
        request.setUser(user);
        request.setRequestType(RequestType.valueOf(requestDTO.getRequestType()));

        // Set dates for allowance request if they are null
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.ALLOWANCE.name())) {
            LocalDate currentDate = LocalDate.now();
            request.setStartDate(currentDate);
            request.setEndDate(currentDate);
        } else {
            request.setStartDate(requestDTO.getStartDate());
            request.setEndDate(requestDTO.getEndDate());
        }

        request.setReason(requestDTO.getReason());
        request.setAllowanceAmount(requestDTO.getAllowanceAmount());
        request.setOvertimeHours(requestDTO.getOvertimeHours());
        request.setStatus(Status.PENDING);

        // Handle leave requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_SICK_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_SICK_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.UNPAID_ANNUAL_LEAVE.name()) ||
                requestDTO.getRequestType().equalsIgnoreCase(RequestType.PAID_ANNUAL_LEAVE.name())) {
            long daysBetween = ChronoUnit.DAYS.between(requestDTO.getStartDate(), requestDTO.getEndDate()) + 1;
            request.setLeaveDays((int) daysBetween);
        }

        // Handle missed attendance requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.MISSED_ATTENDANCE.name())) {
            request.setStartDate(requestDTO.getStartDate());
            request.setEndDate(requestDTO.getStartDate());
        }

        // Save the request
        Request savedRequest = requestRepository.save(request);

        // Create and save a notification
        String message = "Your " + requestDTO.getRequestType().replace("_", " ").toLowerCase() + " request has been created and is pending approval.";
        Notification notification = new Notification(user, message, NotificationType.REQUEST,"ADMIN|MANAGER|USER", Status.PENDING);
        notificationRepository.save(notification);

        return new RequestDTO(savedRequest);

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

            if (hasOverlappingLeave(requestDTO.getUserId(), requestDTO.getStartDate(), requestDTO.getEndDate())) {
                throw new GenericException("User has already applied for leave on the same dates.", HttpStatus.BAD_REQUEST);
            }
        }

        // Validation for Overtime Requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.OVERTIME.name())) {
            if (requestDTO.getOvertimeHours() <= 0 || requestDTO.getOvertimeHours() > 24) {
                throw new GenericException("Overtime hours must be provided and cannot exceed 24 hours.", HttpStatus.BAD_REQUEST);
            }

            if(requestDTO.getStartDate() == null){
                throw new GenericException("Date must be provided",HttpStatus.BAD_REQUEST);
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

        // Validation for Missed Attendance Requests
        if (requestDTO.getRequestType().equalsIgnoreCase(RequestType.MISSED_ATTENDANCE.name())) {


            if (requestDTO.getStartDate() == null) {
                throw new GenericException("Date must be provided for missed attendance requests.", HttpStatus.BAD_REQUEST);
            }
            if (requestDTO.getStartDate().isAfter(LocalDate.now())) {
                throw new GenericException("Missed attendance request date cannot be in the future.", HttpStatus.BAD_REQUEST);
            }
            if (isUserOnLeave(requestDTO.getUserId(), requestDTO.getStartDate())) {
                throw new GenericException("User cannot apply for missed attendance on a leave day.", HttpStatus.BAD_REQUEST);
            }
            if (isMissedAttendanceRequestExists(requestDTO.getUserId(), requestDTO.getStartDate())) {
                throw new GenericException("User has already applied for missed attendance on the same date.", HttpStatus.BAD_REQUEST);
            }
        }
    }



    private boolean isMissedAttendanceRequestExists(Long userId, LocalDate startDate) {
        List<Request> missedAttendanceRequests = requestRepository.findByUserAndRequestTypeAndStartDate(
                userRepository.findById(Math.toIntExact(userId))
                        .orElseThrow(() -> new GenericException("User not found", HttpStatus.NOT_FOUND)),
                RequestType.MISSED_ATTENDANCE, startDate);

        return !missedAttendanceRequests.isEmpty();
    }

    private boolean hasOverlappingLeave(Long userId, LocalDate startDate, LocalDate endDate) {
        return requestRepository.existsByUserIdAndRequestTypeInAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                userId,
                Arrays.asList(RequestType.PAID_SICK_LEAVE, RequestType.UNPAID_SICK_LEAVE, RequestType.PAID_ANNUAL_LEAVE, RequestType.UNPAID_ANNUAL_LEAVE),
                Arrays.asList(Status.PENDING, Status.APPROVED),
                endDate,
                startDate
        );
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
                .orElseThrow(() -> new GenericException("User not found", HttpStatus.NOT_FOUND));


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
                        .orElseThrow(() -> new GenericException("User not found", HttpStatus.NOT_FOUND)),
                RequestType.OVERTIME, date);

        return !overtimeRequests.isEmpty();
    }


    private RequestDTO mapToDTO(Request request) {
        return new RequestDTO(request);
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

  /*  @Override
    public List<RequestDTO> getAllRequestsForLoggedInUser(User user) {
        List<Request> requests = requestRepository.findByUser(user);
        return requests.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }*/

    @Override
    public Page<RequestDTO> getAllRequests(User user, Status status, LocalDate date, String employeeName, Pageable pageable) {
        // Fetch paginated and filtered requests from the repository
        Page<Request> requests = requestRepository.findAllRequests(status, date, employeeName, pageable);

        // Map the Page<Request> to Page<RequestDTO>
        return requests.map(this::mapToDTO);
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
                .orElseThrow(() -> new GenericException("Leave request not found", HttpStatus.NOT_FOUND));

        // Check if the request is already approved
        if (request.getStatus() == Status.APPROVED) {
            throw new GenericException("Leave request is already approved", HttpStatus.BAD_REQUEST);
        }

        // Handle MISSED_ATTENDANCE request approval
        if (request.getRequestType() == RequestType.MISSED_ATTENDANCE) {
            LocalDate attendanceDate = request.getStartDate();
            User user = request.getUser();

            // Check if an attendance record exists for the given date and user
            Optional<Attendance> existingAttendanceOpt = attendanceRepository.findByUserAndDate(user, attendanceDate);

            if (existingAttendanceOpt.isPresent()) {
                // If the record exists and is ABSENT, update it to PRESENT
                Attendance existingAttendance = existingAttendanceOpt.get();
                if (existingAttendance.getStatus() == AttendanceStatus.ABSENT) {
                    existingAttendance.setStatus(AttendanceStatus.PRESENT);
                    attendanceRepository.save(existingAttendance);
                }
            } else {
                // If no record exists, create a new attendance record with status PRESENT
                Attendance newAttendance = new Attendance();
                newAttendance.setUser(user);
                newAttendance.setDate(attendanceDate);
                newAttendance.setStatus(AttendanceStatus.PRESENT);
                attendanceRepository.save(newAttendance);
            }
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
                .orElseThrow(() -> new GenericException("Approver not found", HttpStatus.NOT_FOUND))
                .getId());

        // Save the updated request
        requestRepository.save(request);

        // Create and save a notification
        String message = "Your " + request.getRequestType().name().replace("_", " ").toLowerCase() + " request has been approved.";
        Notification notification = new Notification(request.getUser(), message, NotificationType.REQUEST,"USER", Status.APPROVED);
        notificationRepository.save(notification);

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
                    .orElseThrow(() -> new GenericException("Rejecter not found", HttpStatus.NOT_FOUND))
                    .getId());

            // Save the updated request
            requestRepository.save(request);

            // Create and save a notification
            String message = "Your " + request.getRequestType().name().replace("_", " ").toLowerCase() + " request has been rejected.";
            Notification notification = new Notification(request.getUser(), message, NotificationType.REQUEST, "USER", Status.REJECTED);
            notificationRepository.save(notification);

            return mapToDTO(request);
        }

        throw new GenericException("Leave request not found", HttpStatus.NOT_FOUND);
    }

    //To cancel a request created by user
    //Employee can cancel the request they created
    @Override
    public boolean cancelRequest(Long requestId, String username) {
        Optional<Request> optionalRequest = requestRepository.findById(requestId);

        if (optionalRequest.isPresent()) {
            Request request = optionalRequest.get();

            // Only allow cancellation if the request is still PENDING
            if (!request.getStatus().equals(Status.PENDING)) {
                throw new IllegalStateException("Only pending requests can be cancelled.");
            }

            // Ensure only the request creator can cancel it
            if (!request.getUser().getUsername().equals(username)) {
                throw new AccessDeniedException("You are not authorized to cancel this request.", HttpStatus.FORBIDDEN);
            }

            // Perform deletion
            requestRepository.delete(request);
            return true;
        } else {
            throw new RuntimeException("Request not found.");
        }
    }

    //Here filter by date status and pagination concept is added
    @Override
    public Page<RequestDTO> getAllRequestsForLoggedInUser(User user, Status status, LocalDate date, Pageable pageable) {
        // Fetch paginated and filtered requests from the repository
        Page<Request> requests = requestRepository.findByUserAndFilters(user, status, date, pageable);

        // Map the Page<Request> to Page<RequestDTO>
        return requests.map(this::mapToDTO);
    }
}
