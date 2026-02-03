package com.projectfaust.service;

import com.projectfaust.dto.request.AgentOnboardingRequest;
import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import com.projectfaust.mapper.UserMapper;
import com.projectfaust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public ProfileResponse getProfileByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Subject not found in archive."));
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> getArchive() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfileResponse getDossier(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new RuntimeException("Access denied to dossier: " + id));
    }

    @Transactional
    public void onboardAgent(AgentOnboardingRequest request) {
        User agent = new User();
        agent.setFullName(request.codename());
        agent.setEmail(request.officialEmail());
        agent.setClearance(request.assignedLevel());
        agent.setRole(request.requiresFieldAccess() ? "FIELD_OPERATIVE" : "ANALYST");
        agent.setStatus("ACTIVE");
        agent.setAdmin(false);
        // Defaultní heslo pro první login
        agent.setPassword("INIT_SECRET_2026");

        userRepository.save(agent);
    }
}
