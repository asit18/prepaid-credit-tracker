package com.yourcompany.credittracker.service;

public interface EncryptionService {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
