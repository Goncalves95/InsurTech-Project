package ch.insurtech.platform.ocr.domain.model;

import ch.insurtech.platform.claim.domain.model.TarmedPosition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExtractedInvoiceData(
        UUID claimId,
        String physicianGln,
        String physicianName,
        LocalDate treatmentDate,
        BigDecimal totalAmount,
        List<TarmedPosition> positions
) {}
