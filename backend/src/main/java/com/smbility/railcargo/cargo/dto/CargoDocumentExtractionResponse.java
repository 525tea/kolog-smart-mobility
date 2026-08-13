package com.smbility.railcargo.cargo.dto;

import java.util.List;

public record CargoDocumentExtractionResponse(
        String fileName,
        String mimeType,
        String provider,
        String extractedText,
        int pageOrSheetCount,
        int formFieldCount,
        int tableCount,
        List<String> warnings
) {
}
