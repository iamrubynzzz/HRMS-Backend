package com.hrms.backend;

import com.hrms.backend.entities.Role;
import com.hrms.backend.entities.User;
import com.hrms.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class HrmsApplication implements CommandLineRunner {

	@Autowired
	private UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(HrmsApplication.class, args);
	}

	@Override
	public void run(String... args) {
		User adminAccount = userRepository.findByRole(Role.Admin);
		if (adminAccount == null) {
			// Create a new user
			User user = new User();

			// Set user properties directly (no need to manually call setters if Lombok is used)
			user.setEmail("rubina@gmail.com");
			user.setName("Rubina Thapa");
			user.setRole(Role.Manager);
			user.setPassword(new BCryptPasswordEncoder().encode("rubina123"));

			// Save the user
			userRepository.save(user);
		}
	}
}
