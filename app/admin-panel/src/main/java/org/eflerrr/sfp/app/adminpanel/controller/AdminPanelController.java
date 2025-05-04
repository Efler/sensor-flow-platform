package org.eflerrr.sfp.app.adminpanel.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.eflerrr.sfp.app.adminpanel.dto.DeviceDto;
import org.eflerrr.sfp.app.adminpanel.dto.ModelParamsDto;
import org.eflerrr.sfp.app.adminpanel.dto.SubscriptionDto;
import org.eflerrr.sfp.app.adminpanel.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// todo!
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@OpenAPIDefinition(
        info = @Info(
                title = "SFP Admin Panel API",
                version = "1.1",
                description = "HTTP API for the SFP Admin Panel"
        )
)
public class AdminPanelController {

    private final AdminService adminService;

    // === DEVICE MANAGEMENT ===

    @Operation(summary = "Register a new device", description = "Registers a new device in the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping("/devices")
    public ResponseEntity<?> registerDevice(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Device details", required = true) DeviceDto deviceDto) {
        return ResponseEntity.ok(adminService.registerDevice(deviceDto));
    }

    @Operation(summary = "Reset device secret", description = "Resets the secret for a specific device.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Secret reset successfully"),
            @ApiResponse(responseCode = "404", description = "Device not found", content = @Content)
    })
    @PostMapping("/devices/{deviceId}/reset-secret")
    public ResponseEntity<?> resetSecret(@PathVariable String deviceId) {
        return ResponseEntity.ok(adminService.resetSecret(deviceId));
    }

    @Operation(summary = "List all devices", description = "Retrieves a list of all registered devices.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of devices retrieved successfully")
    })
    @GetMapping("/devices")
    public ResponseEntity<List<DeviceDto>> listDevices() {
        adminService.getAllDevices();
        return ResponseEntity.ok(List.of(new DeviceDto()));
    }

    @Operation(summary = "Subscribe a user", description = "Subscribes a user to notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User subscribed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping("/subscriptions")
    public ResponseEntity<?> subscribe(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Subscription details", required = true) SubscriptionDto subscriptionDto) {
        adminService.subscribeUser(subscriptionDto);
        return ResponseEntity.ok("User subscribed");
    }

    @Operation(summary = "Unsubscribe a user", description = "Unsubscribes a user from notifications.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User unsubscribed successfully"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @DeleteMapping("/subscriptions")
    public ResponseEntity<?> unsubscribe(@RequestParam String chatId) {
        adminService.unsubscribeUser(chatId);
        return ResponseEntity.ok("User unsubscribed");
    }

    @Operation(summary = "List all subscribers", description = "Retrieves a list of all subscribers.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of subscribers retrieved successfully")
    })
    @GetMapping("/subscriptions")
    public ResponseEntity<List<SubscriptionDto>> listSubscribers() {
        adminService.getAllSubscribers();
        return ResponseEntity.ok(List.of(new SubscriptionDto()));
    }

    // === MODEL PARAMETERS ===

    @Operation(summary = "Update model parameters", description = "Updates the parameters of the model.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Parameters updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", content = @Content)
    })
    @PostMapping("/model-params")
    public ResponseEntity<?> updateModelParams(@RequestBody(description = "Model parameters", required = true) ModelParamsDto dto) {
        adminService.updateModelParams(dto);
        return ResponseEntity.ok("Parameters updated");
    }

    @Operation(summary = "List model parameters", description = "Retrieves a list of all model parameters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of model parameters retrieved successfully")
    })
    @GetMapping("/model-params")
    public ResponseEntity<List<ModelParamsDto>> listModelParams() {
        adminService.getAllModelParams();
        return ResponseEntity.ok(List.of(new ModelParamsDto()));
    }

}
