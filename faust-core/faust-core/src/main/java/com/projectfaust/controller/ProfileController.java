package com.projectfaust.controller;

import com.projectfaust.dto.request.AgentOnboardingRequest;
import com.projectfaust.dto.response.ProfileResponse;
import com.projectfaust.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestParam String email) {
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }

    @GetMapping("/archive")
    public ResponseEntity<List<ProfileResponse>> getArchive() {
        return ResponseEntity.ok(userService.getArchive());
    }

    @GetMapping("/dossier/{id}")
    public ResponseEntity<ProfileResponse> getDossier(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getDossier(id));
    }

    @PostMapping("/onboard")
    public ResponseEntity<Void> onboard(@RequestBody AgentOnboardingRequest request) {
        userService.onboardAgent(request);
        return ResponseEntity.ok().build();
    }
}
