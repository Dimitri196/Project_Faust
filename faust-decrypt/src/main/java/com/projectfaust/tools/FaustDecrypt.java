package com.projectfaust.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

/**
 * FAUST Steganography Decryption Tool — standalone CLI.
 *
 * <p>Extracts and decrypts intelligence payloads from steganographic PNG images.
 * Requires no server connection — runs completely offline.</p>
 *
 * <p>Usage:
 * <pre>
 *   java -jar faust-decrypt.jar <image.png> <passphrase>
 *   java -jar faust-decrypt.jar <image.png> <passphrase> --output report.json
 * </pre>
 *
 * <p>Dependencies: Jackson (jackson-databind), standard Java crypto (built-in).
 * Compile: javac -cp jackson-databind.jar FaustDecrypt.java
 * Run:     java -cp .:jackson-databind.jar com.projectfaust.tools.FaustDecrypt image.png passphrase
 *
 * @author Dimitri / Project Faust
 */
public class FaustDecrypt {

    // Must match SteganographyService constants exactly
    private static final String  AES_ALGORITHM     = "AES/GCM/NoPadding";
    private static final String  KEY_ALGORITHM     = "PBKDF2WithHmacSHA256";
    private static final int     GCM_TAG_LENGTH    = 128;
    private static final int     GCM_IV_LENGTH     = 12;
    private static final int     SALT_LENGTH       = 16;
    private static final int     KEY_LENGTH        = 256;
    private static final int     PBKDF2_ITERATIONS = 310_000;
    private static final byte[]  MAGIC_HEADER      =
            "FAUST_STEG_V1".getBytes(StandardCharsets.UTF_8);

    public static void main(String[] args) {
        printBanner();

        if (args.length < 2) {
            System.err.println("Usage: faust-decrypt <image.png> <passphrase> [--output file.json]");
            System.err.println("       faust-decrypt <image.png> <passphrase> --info");
            System.exit(1);
        }

        String imagePath  = args[0];
        String passphrase = args[1];
        String outputPath = null;
        boolean infoOnly  = false;

        for (int i = 2; i < args.length; i++) {
            if ("--output".equals(args[i]) && i + 1 < args.length) {
                outputPath = args[++i];
            } else if ("--info".equals(args[i])) {
                infoOnly = true;
            }
        }

        try {
            System.out.println("[*] Loading image: " + imagePath);
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                System.err.println("[!] Error: Cannot read image. Ensure it is a valid PNG file.");
                System.exit(1);
            }
            System.out.printf("[*] Image loaded: %dx%d px%n",
                    image.getWidth(), image.getHeight());

            if (infoOnly) {
                printImageInfo(image);
                return;
            }

            System.out.println("[*] Extracting payload...");
            byte[] payload = extractAndDecrypt(image, passphrase);

            // Pretty print JSON
            ObjectMapper mapper = new ObjectMapper()
                    .enable(SerializationFeature.INDENT_OUTPUT);
            Object json = mapper.readValue(payload, Object.class);
            String prettyJson = mapper.writeValueAsString(json);

            if (outputPath != null) {
                Files.writeString(Path.of(outputPath), prettyJson);
                System.out.println("[+] Payload saved to: " + outputPath);
            } else {
                System.out.println("\n[+] ═══════════════ DECRYPTED PAYLOAD ═══════════════");
                System.out.println(prettyJson);
                System.out.println("[+] ═════════════════════════════════════════════════");
            }

            System.out.println("[+] Decryption successful.");

        } catch (WrongPassphraseException e) {
            System.err.println("[!] DECRYPTION FAILED: Wrong passphrase or corrupted image.");
            System.exit(2);
        } catch (NoPayloadException e) {
            System.err.println("[!] NO PAYLOAD: This image does not contain a FAUST payload.");
            System.exit(3);
        } catch (Exception e) {
            System.err.println("[!] Error: " + e.getMessage());
            System.exit(1);
        }
    }

    // ── Extract & decrypt ─────────────────────────────────────────────────────

    private static byte[] extractAndDecrypt(BufferedImage image, String passphrase)
            throws Exception {

        // 1. Extract header
        int headerSize = MAGIC_HEADER.length + SALT_LENGTH + GCM_IV_LENGTH + 4;
        byte[] headerBytes = extractBits(image, headerSize);

        // 2. Validate magic header
        ByteBuffer buf = ByteBuffer.wrap(headerBytes);
        byte[] magic = new byte[MAGIC_HEADER.length];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC_HEADER)) {
            throw new NoPayloadException();
        }

        // 3. Read salt, IV, data length
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv   = new byte[GCM_IV_LENGTH];
        buf.get(salt);
        buf.get(iv);
        int dataLength = buf.getInt();

        System.out.printf("[*] Payload detected: %d bytes encrypted data%n", dataLength);

        if (dataLength <= 0 || dataLength > image.getWidth() * image.getHeight() * 3 / 8) {
            throw new NoPayloadException();
        }

        // 4. Extract full encrypted data
        byte[] fullFrame = extractBits(image, headerSize + dataLength);
        byte[] encrypted = new byte[dataLength];
        System.arraycopy(fullFrame, headerSize, encrypted, 0, dataLength);

        // 5. Derive key and decrypt
        System.out.println("[*] Deriving key (PBKDF2, 310000 iterations)...");
        SecretKey key = deriveKey(passphrase, salt);

        System.out.println("[*] Decrypting (AES-256-GCM)...");
        byte[] compressed;
        try {
            compressed = decrypt(encrypted, key, iv);
        } catch (Exception e) {
            throw new WrongPassphraseException();
        }

        // 6. Decompress
        System.out.println("[*] Decompressing...");
        return decompress(compressed);
    }

    // ── LSB extraction ────────────────────────────────────────────────────────

    private static byte[] extractBits(BufferedImage image, int byteCount) {
        byte[] data     = new byte[byteCount];
        int width       = image.getWidth();
        int height      = image.getHeight();
        int bitIndex    = 0;
        int totalBits   = byteCount * 8;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= totalBits) break outer;
                int pixel = image.getRGB(x, y);
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >>  8) & 0xFF;
                int b = (pixel      ) & 0xFF;
                if (bitIndex < totalBits) setBit(data, bitIndex++, r & 1);
                if (bitIndex < totalBits) setBit(data, bitIndex++, g & 1);
                if (bitIndex < totalBits) setBit(data, bitIndex++, b & 1);
            }
        }
        return data;
    }

    private static void setBit(byte[] data, int bitIndex, int bit) {
        int byteIndex = bitIndex / 8;
        int bitPos    = 7 - (bitIndex % 8);
        data[byteIndex] = (byte) ((data[byteIndex] & ~(1 << bitPos)) | (bit << bitPos));
    }

    // ── Crypto ────────────────────────────────────────────────────────────────

    private static SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private static byte[] decrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    private static byte[] decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gzip.transferTo(bos);
            return bos.toByteArray();
        }
    }

    // ── Info mode ─────────────────────────────────────────────────────────────

    private static void printImageInfo(BufferedImage image) {
        int capacity = (image.getWidth() * image.getHeight() * 3) / 8;
        System.out.println("\n[i] Image Information:");
        System.out.printf("    Dimensions:  %dx%d px%n", image.getWidth(), image.getHeight());
        System.out.printf("    LSB capacity: %d bytes (%.1f KB)%n",
                capacity, capacity / 1024.0);

        // Check for FAUST magic header
        try {
            byte[] headerBytes = extractBits(image, MAGIC_HEADER.length);
            if (Arrays.equals(headerBytes, MAGIC_HEADER)) {
                System.out.println("    FAUST payload: DETECTED ✓");
            } else {
                System.out.println("    FAUST payload: Not detected");
            }
        } catch (Exception e) {
            System.out.println("    FAUST payload: Cannot determine");
        }
    }

    // ── Banner ────────────────────────────────────────────────────────────────

    private static void printBanner() {
        System.out.println(
                "+=======================================+\n" +
                        "|  FAUST_SYS - Steganography Decrypt    |\n" +
                        "|  Global Intelligence Platform v1.0    |\n" +
                        "|  RESTRICTED - AUTHORIZED USE ONLY     |\n" +
                        "+=======================================+\n"
        );
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    static class WrongPassphraseException extends Exception {}
    static class NoPayloadException extends Exception {}
}
