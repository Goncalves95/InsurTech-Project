package ch.insurtech.platform.claim.api;

import ch.insurtech.platform.claim.api.dto.ClaimResponse;
import ch.insurtech.platform.claim.api.mapper.ClaimMapper;
import ch.insurtech.platform.claim.application.ClaimApplicationService;
import ch.insurtech.platform.claim.domain.model.Claim;
import ch.insurtech.platform.claim.domain.model.ClaimStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/claims")
@Validated
@Tag(name = "Claims", description = "Medical invoice claim submission and retrieval")
public class ClaimController {

    private final ClaimApplicationService claimService;
    private final ClaimMapper claimMapper;

    public ClaimController(ClaimApplicationService claimService, ClaimMapper claimMapper) {
        this.claimService = claimService;
        this.claimMapper = claimMapper;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit a medical invoice claim", description = "Upload a PDF/image invoice for async OCR processing and validation")
    public ResponseEntity<ClaimResponse> submitClaim(
            @RequestParam @NotBlank String policyHolderId,
            @RequestParam MultipartFile document) {

        Claim claim = claimService.submitClaim(policyHolderId, document);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(claimMapper.toResponse(claim));
    }

    @GetMapping("/{claimId}")
    @Operation(summary = "Get claim by ID")
    public ResponseEntity<ClaimResponse> getById(@PathVariable UUID claimId) {
        return ResponseEntity.ok(claimMapper.toResponse(claimService.findById(claimId)));
    }

    @GetMapping
    @Operation(summary = "List claims for a policy holder")
    public ResponseEntity<List<ClaimResponse>> listByPolicyHolder(
            @RequestParam @NotBlank String policyHolderId) {
        return ResponseEntity.ok(claimMapper.toResponseList(claimService.findByPolicyHolder(policyHolderId)));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('SCOPE_backoffice')")
    @Operation(summary = "List claims by status — backoffice only")
    public ResponseEntity<List<ClaimResponse>> listByStatus(@PathVariable ClaimStatus status) {
        return ResponseEntity.ok(claimMapper.toResponseList(claimService.findByStatus(status)));
    }
}
