package com.smbility.railcargo.cargo.dto;

public record CargoMsdsFile(String fileName, String contentType, byte[] data) {
}
