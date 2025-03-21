package rs.raf.user_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import rs.raf.user_service.domain.dto.AuthorizedPersonelDto;
import rs.raf.user_service.domain.dto.CreateAuthorizedPersonelDto;
import rs.raf.user_service.service.AuthorizedPersonelService;
import rs.raf.user_service.service.ClientService;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/authorized-personnel")
@Tag(name = "Authorized Personnel Management", description = "API for managing authorized personnel for companies")
@AllArgsConstructor
public class AuthorizedPersonelController {

    private final AuthorizedPersonelService authorizedPersonelService;

    // @todo za sve ovo koristiti permisije umesto role pls

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Create new authorized personnel", description = "Creates new authorized personnel for a company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created authorized personnel"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "403", description = "Not authorized to create authorized personnel for this company")
    })
    @PostMapping
    public ResponseEntity<?> createAuthorizedPersonnel(
            @RequestBody @Valid CreateAuthorizedPersonelDto createAuthorizedPersonelDto) {
        try {
            AuthorizedPersonelDto createdPersonnel = authorizedPersonelService
                    .createAuthorizedPersonel(createAuthorizedPersonelDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPersonnel);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Get authorized personnel for company", description = "Gets all authorized personnel for a company")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved authorized personnel"),
            @ApiResponse(responseCode = "404", description = "Company not found")
    })
    @GetMapping("/company/{companyId}")
    public ResponseEntity<?> getAuthorizedPersonnelByCompany(@PathVariable Long companyId) {
        try {
            List<AuthorizedPersonelDto> authorizedPersonnel = authorizedPersonelService
                    .getAuthorizedPersonelByCompany(companyId);
            return ResponseEntity.ok(authorizedPersonnel);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Get authorized personnel by ID", description = "Gets an authorized personnel by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved authorized personnel"),
            @ApiResponse(responseCode = "404", description = "Authorized personnel not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getAuthorizedPersonnelById(@PathVariable Long id) {
        try {
            AuthorizedPersonelDto authorizedPersonnel = authorizedPersonelService.getAuthorizedPersonelById(id);
            return ResponseEntity.ok(authorizedPersonnel);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Update authorized personnel", description = "Updates an authorized personnel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated authorized personnel"),
            @ApiResponse(responseCode = "400", description = "Invalid data"),
            @ApiResponse(responseCode = "403", description = "Not authorized to update authorized personnel for this company"),
            @ApiResponse(responseCode = "404", description = "Authorized personnel not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAuthorizedPersonnel(@PathVariable Long id,
            @RequestBody @Valid CreateAuthorizedPersonelDto updateDto) {
        try {
            AuthorizedPersonelDto updatedPersonnel = authorizedPersonelService.updateAuthorizedPersonel(id, updateDto);
            return ResponseEntity.ok(updatedPersonnel);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SUPERVISOR', 'AGENT')")
    @Operation(summary = "Delete authorized personnel", description = "Deletes an authorized personnel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted authorized personnel"),
            @ApiResponse(responseCode = "403", description = "Not authorized to delete authorized personnel for this company"),
            @ApiResponse(responseCode = "404", description = "Authorized personnel not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAuthorizedPersonnel(@PathVariable Long id) {
        try {
            authorizedPersonelService.deleteAuthorizedPersonel(id);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
