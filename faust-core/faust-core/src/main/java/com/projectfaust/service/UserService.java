package com.projectfaust.service;

import com.projectfaust.dto.request.AgentOnboardingRequest;
import com.projectfaust.dto.request.UpdateProfileRequest;
import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.entity.User;
import com.projectfaust.mapper.UserMapper;
import com.projectfaust.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public ProfileResponse onboardAgent(AgentOnboardingRequest request) {
        User agent = new User();
        agent.setFullName(request.codename());
        agent.setEmail(request.officialEmail());
        agent.setClearance(request.assignedLevel());
        agent.setRole(request.requiresFieldAccess() ? "FIELD_OPERATIVE" : "ANALYST");
        agent.setStatus("ACTIVE");
        agent.setAdmin(false);
        agent.setPassword("INIT_SECRET_2026");

        // Důležité: inicializace prázdného seznamu
        agent.setTechStack(new ArrayList<>());

        User saved = userRepository.save(agent);
        return userMapper.toResponse(saved);
    }

    @Transactional
    public ProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found: " + id));

        // Parciální updaty - mění se jen to, co není null
        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.role() != null) user.setRole(request.role());
        if (request.clearance() != null) user.setClearance(request.clearance());
        if (request.status() != null) user.setStatus(request.status());

        if (request.techStack() != null) {
            // U ElementCollection je nejbezpečnější clear() a addAll()
            user.getTechStack().clear();
            user.getTechStack().addAll(request.techStack());
        }

        return userMapper.toResponse(userRepository.save(user));
    }
}
