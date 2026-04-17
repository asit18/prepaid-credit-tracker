package com.yourcompany.credittracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.ArrayList;
import java.util.List;

public record CustomerRequest(@NotBlank String name, String address, String email, @NotBlank String phone,
                              @Valid List<ContactRequest> contacts) {
    public List<ContactRequest> safeContacts() {
        return contacts == null ? new ArrayList<>() : contacts;
    }
}
