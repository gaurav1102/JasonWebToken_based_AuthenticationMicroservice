package authservice.controller;

import authservice.response.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DemoController {

    @Operation(summary = "Protected endpoint for authenticated users")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/me")
    public MessageResponse currentUser(Authentication authentication) {
        return new MessageResponse("Authenticated as " + authentication.getName());
    }

    @Operation(summary = "Protected endpoint for admin users")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public MessageResponse adminOnly() {
        return new MessageResponse("Admin access granted");
    }
}
