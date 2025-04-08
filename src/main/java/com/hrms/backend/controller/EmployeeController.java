package com.hrms.backend.controller;

import com.hrms.backend.dto.LeaveBalanceDTO;
import com.hrms.backend.dto.ManagerLeaveBalanceResponse;
import com.hrms.backend.dto.UserRequestDTO;
import com.hrms.backend.dto.UserResponseDTO;
import com.hrms.backend.entities.Attendance;
import com.hrms.backend.entities.User;
import com.hrms.backend.entities.UserInfo;
import com.hrms.backend.exception.GenericException;
import com.hrms.backend.repository.EmployeeManagerRepository;
import com.hrms.backend.repository.UserInfoRepository;
import com.hrms.backend.services.EmployeeService;
import com.hrms.backend.services.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class EmployeeController {


    private final EmployeeService employeeService;
    private final UserService userService;
    private  final UserInfoRepository userInfoRepository;
    private final EmployeeManagerRepository employeeManagerRepository;
    @GetMapping
    public ResponseEntity<String> sayHello(){
        return ResponseEntity.ok("Hi Employee");
    }


    // Create a new employee
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<String> createUser(@RequestBody UserRequestDTO userRequestDTO) {
        employeeService.createUser(userRequestDTO, new UserInfo());
        return ResponseEntity.ok("User created successfully");
    }


    // Get an employee by ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Integer id) {
        UserResponseDTO employee = employeeService.getUserById(id);
        return ResponseEntity.ok(employee);
    }

    // APi to show the list of employees in the attendance table
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Debugging output
        System.out.println("Fetching users with filter - Name: " + name + ", Page: " + page + ", Size: " + size);

        // Fetch paginated and filtered users
        Page<UserResponseDTO> usersPage = employeeService.getAllUsers(name, page, size);

        // Debugging output
        System.out.println("Total Users Retrieved: " + usersPage.getTotalElements());

        // Return the response with HTTP 200 OK
        return ResponseEntity.ok(usersPage);
    }


    // Update an existing employee
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateUser(@PathVariable Integer id, @RequestBody UserRequestDTO userRequestDTO) {
        employeeService.updateUser(id, userRequestDTO);
        return ResponseEntity.ok("User information updated successfully");
    }

    // Delete an employee
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Integer id) {
        employeeService.deleteUser(id);
        return ResponseEntity.ok("Employee deactivated successfully");
    }



    @GetMapping("/details")
    public Integer getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Check if the user is authenticated
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            return user.getId();  // Return the userId
        }

        throw new RuntimeException("User not authenticated");
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getEmployeeStats() {
        long totalEmployees = employeeService.countTotalEmployees();
        long totalMale = employeeService.countEmployeesByGender("Male");
        long totalFemale = employeeService.countEmployeesByGender("Female");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEmployees", totalEmployees);
        stats.put("totalMale", totalMale);
        stats.put("totalFemale", totalFemale);

        return ResponseEntity.ok(stats);
    }


// For leave balance of individual employee
    @GetMapping("/my-leave-balance")
    @PreAuthorize("hasRole('EMPLOYEE') or hasRole('MANAGER')")
    public ResponseEntity<LeaveBalanceDTO> getMyLeaveBalance(Principal principal) {
        Optional<User> userOptional = userService.findByUsername(principal.getName());
        if (userOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = userOptional.get();
        Optional<UserInfo> userInfoOptional = userInfoRepository.findByUserId(user.getId());

        if (userInfoOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserInfo userInfo = userInfoOptional.get();
        LeaveBalanceDTO dto = new LeaveBalanceDTO(
                user.getName(),
                userInfo.getAnnualLeaveBalance(),
                userInfo.getSickLeaveBalance()
        );

        return ResponseEntity.ok(dto);
    }

    // To show the leave balance of manager as well as their assigned employee
    @GetMapping("/manager/leave-balances")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ManagerLeaveBalanceResponse> getManagerAndEmployeesLeaveBalances(Principal principal) {
        // 1. Get manager details
        Optional<User> managerOptional = userService.findByUsername(principal.getName());
        if (managerOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User manager = managerOptional.get();

        // 2. Get manager's leave balance
        Optional<UserInfo> managerInfoOptional = userInfoRepository.findByUserId(manager.getId());
        if (managerInfoOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        UserInfo managerInfo = managerInfoOptional.get();
        LeaveBalanceDTO managerLeaveBalance = new LeaveBalanceDTO(
                manager.getName(),
                managerInfo.getAnnualLeaveBalance(),
                managerInfo.getSickLeaveBalance()
        );

        // 3. Get assigned employees' leave balances
        List<Integer> employeeIds = employeeManagerRepository.findEmployeeIdsByManagerId(manager.getId());
        List<UserInfo> employeeInfos = userInfoRepository.findByUserIdIn(employeeIds);

        List<LeaveBalanceDTO> employeeLeaveBalances = employeeInfos.stream()
                .map(info -> new LeaveBalanceDTO(info.getUser().getName(), info.getAnnualLeaveBalance(), info.getSickLeaveBalance()))
                .collect(Collectors.toList());

        // 4. Prepare response
        ManagerLeaveBalanceResponse response = new ManagerLeaveBalanceResponse(managerLeaveBalance, employeeLeaveBalances);

        return ResponseEntity.ok(response);
    }

}
