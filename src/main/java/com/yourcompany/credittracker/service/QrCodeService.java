package com.yourcompany.credittracker.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Service
public class QrCodeService {
    public String generateBase64Png(String contents) {
        try {
            var matrix = new QRCodeWriter().encode(contents, BarcodeFormat.QR_CODE, 256, 256,
                    Map.of(EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate MFA QR code", ex);
        }
    }
}
