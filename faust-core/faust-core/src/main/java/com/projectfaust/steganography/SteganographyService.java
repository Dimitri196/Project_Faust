package com.projectfaust.steganography;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;

/**
 * Steganography service for embedding encrypted intelligence payloads
 * into PNG images using Least Significant Bit (LSB) technique.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Serialize payload to JSON</li>
 *   <li>Compress with GZIP</li>
 *   <li>Encrypt with AES-256-GCM (authenticated encryption)</li>
 *   <li>Embed into PNG pixel LSBs</li>
 * </ol>
 *
 * <p>Each pixel's RGB channels contribute 1 bit each (3 bits per pixel).
 * The first 32 bits encode the payload length.
 * Alpha channel is never modified to avoid detection.</p>
 *
 * <p>Capacity: a 1920x1080 PNG can store ~777KB of encrypted data.</p>
 *
 * @author Dimitri / Project Faust
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SteganographyService {

    private static final String AES_ALGORITHM    = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM    = "PBKDF2WithHmacSHA256";
    private static final int    GCM_TAG_LENGTH   = 128;
    private static final int    GCM_IV_LENGTH    = 12;
    private static final int    SALT_LENGTH      = 16;
    private static final int    KEY_LENGTH       = 256;
    private static final int    PBKDF2_ITERATIONS = 310_000; // OWASP recommended
    private static final byte[] MAGIC_HEADER     = "FAUST_STEG_V1".getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    // ── Embed ─────────────────────────────────────────────────────────────────

    /**
     * Embeds an encrypted payload into a PNG image using LSB steganography.
     *
     * @param image      the carrier PNG image (must be large enough).
     * @param payload    the intelligence data to embed (any serializable object).
     * @param passphrase the encryption passphrase (never stored).
     * @return modified BufferedImage with embedded payload.
     * @throws SteganographyException if the image is too small or embedding fails.
     */
    public BufferedImage embed(BufferedImage image, Object payload, String passphrase)
            throws SteganographyException {

        log.info("STEG: Starting LSB embed into {}x{} image.",
                image.getWidth(), image.getHeight());

        try {
            // 1. Serialize payload to JSON
            byte[] json = objectMapper.writeValueAsBytes(payload);
            log.debug("STEG: Payload JSON size: {} bytes.", json.length);

            // 2. Compress
            byte[] compressed = compress(json);
            log.debug("STEG: Compressed size: {} bytes.", compressed.length);

            // 3. Encrypt with AES-256-GCM
            byte[] salt = generateRandom(SALT_LENGTH);
            byte[] iv   = generateRandom(GCM_IV_LENGTH);
            SecretKey key = deriveKey(passphrase, salt);
            byte[] encrypted = encrypt(compressed, key, iv);
            log.debug("STEG: Encrypted size: {} bytes.", encrypted.length);

            // 4. Build frame:
            // [MAGIC_HEADER][SALT(16)][IV(12)][LENGTH(4)][ENCRYPTED_DATA]
            ByteBuffer frame = ByteBuffer.allocate(
                    MAGIC_HEADER.length + SALT_LENGTH + GCM_IV_LENGTH + 4 + encrypted.length);
            frame.put(MAGIC_HEADER);
            frame.put(salt);
            frame.put(iv);
            frame.putInt(encrypted.length);
            frame.put(encrypted);
            byte[] data = frame.array();

            // 5. Check capacity
            int capacity = (image.getWidth() * image.getHeight() * 3) / 8;
            if (data.length > capacity) {
                throw new SteganographyException(String.format(
                        "Image too small. Need %d bytes, capacity is %d bytes. " +
                                "Use a larger image (minimum %dx%d px).",
                        data.length, capacity,
                        (int) Math.ceil(Math.sqrt((double) data.length * 8 / 3)),
                        (int) Math.ceil(Math.sqrt((double) data.length * 8 / 3))));
            }

            // 6. Embed bits into LSBs
            BufferedImage result = copyImage(image);
            embedBits(result, data);

            log.info("STEG: Successfully embedded {} bytes into image. Capacity used: {}/{}.",
                    data.length, data.length, capacity);
            return result;

        } catch (SteganographyException e) {
            throw e;
        } catch (Exception e) {
            throw new SteganographyException("Embedding failed: " + e.getMessage(), e);
        }
    }

    // ── Extract ───────────────────────────────────────────────────────────────

    /**
     * Extracts and decrypts a payload from a steganographic PNG image.
     *
     * @param image      the carrier image containing the hidden payload.
     * @param passphrase the decryption passphrase.
     * @param targetType the expected payload type for deserialization.
     * @return the decrypted and deserialized payload.
     * @throws SteganographyException if extraction or decryption fails.
     */
    public <T> T extract(BufferedImage image, String passphrase, Class<T> targetType)
            throws SteganographyException {

        log.info("STEG: Starting LSB extract from {}x{} image.",
                image.getWidth(), image.getHeight());

        try {
            // 1. Extract header bytes first to get data length
            int headerSize = MAGIC_HEADER.length + SALT_LENGTH + GCM_IV_LENGTH + 4;
            byte[] headerBytes = extractBits(image, headerSize);

            // 2. Validate magic header
            ByteBuffer headerBuf = ByteBuffer.wrap(headerBytes);
            byte[] magic = new byte[MAGIC_HEADER.length];
            headerBuf.get(magic);
            if (!java.util.Arrays.equals(magic, MAGIC_HEADER)) {
                throw new SteganographyException(
                        "Invalid magic header — image does not contain a FAUST payload.");
            }

            // 3. Read salt, IV, length
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv   = new byte[GCM_IV_LENGTH];
            headerBuf.get(salt);
            headerBuf.get(iv);
            int dataLength = headerBuf.getInt();

            if (dataLength <= 0 || dataLength > image.getWidth() * image.getHeight() * 3 / 8) {
                throw new SteganographyException("Invalid payload length: " + dataLength);
            }

            // 4. Extract full frame
            byte[] fullFrame = extractBits(image, headerSize + dataLength);
            byte[] encrypted = new byte[dataLength];
            System.arraycopy(fullFrame, headerSize, encrypted, 0, dataLength);

            // 5. Decrypt
            SecretKey key = deriveKey(passphrase, salt);
            byte[] compressed = decrypt(encrypted, key, iv);

            // 6. Decompress
            byte[] json = decompress(compressed);

            // 7. Deserialize
            T result = objectMapper.readValue(json, targetType);
            log.info("STEG: Successfully extracted and decrypted {} bytes.", dataLength);
            return result;

        } catch (SteganographyException e) {
            throw e;
        } catch (Exception e) {
            throw new SteganographyException("Extraction failed — wrong passphrase or corrupted image: "
                    + e.getMessage(), e);
        }
    }

    // ── LSB core ──────────────────────────────────────────────────────────────

    /**
     * Embeds bytes into the LSB of RGB channels of each pixel.
     * 3 bits per pixel (1 per channel), alpha channel untouched.
     */
    private void embedBits(BufferedImage image, byte[] data) {
        int width  = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int totalBits = data.length * 8;

        outer:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitIndex >= totalBits) break outer;

                int pixel = image.getRGB(x, y);
                int a = (pixel >> 24) & 0xFF;
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >>  8) & 0xFF;
                int b = (pixel      ) & 0xFF;

                // Embed 1 bit in R
                if (bitIndex < totalBits) {
                    r = (r & 0xFE) | getBit(data, bitIndex++);
                }
                // Embed 1 bit in G
                if (bitIndex < totalBits) {
                    g = (g & 0xFE) | getBit(data, bitIndex++);
                }
                // Embed 1 bit in B
                if (bitIndex < totalBits) {
                    b = (b & 0xFE) | getBit(data, bitIndex++);
                }

                image.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
    }

    /**
     * Extracts bytes from the LSB of RGB channels.
     */
    private byte[] extractBits(BufferedImage image, int byteCount) {
        byte[] data = new byte[byteCount];
        int width  = image.getWidth();
        int height = image.getHeight();
        int bitIndex = 0;
        int totalBits = byteCount * 8;

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

    private int getBit(byte[] data, int bitIndex) {
        return (data[bitIndex / 8] >> (7 - (bitIndex % 8))) & 1;
    }

    private void setBit(byte[] data, int bitIndex, int bit) {
        int byteIndex = bitIndex / 8;
        int bitPos    = 7 - (bitIndex % 8);
        data[byteIndex] = (byte) ((data[byteIndex] & ~(1 << bitPos)) | (bit << bitPos));
    }

    // ── Crypto ────────────────────────────────────────────────────────────────

    private SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        KeySpec spec = new PBEKeySpec(
                passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    private byte[] encrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
        return cipher.doFinal(data);
    }

    // ── Compression ───────────────────────────────────────────────────────────

    private byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data);
        }
        return bos.toByteArray();
    }

    private byte[] decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            gzip.transferTo(bos);
            return bos.toByteArray();
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private byte[] generateRandom(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private BufferedImage copyImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        copy.getGraphics().drawImage(source, 0, 0, null);
        return copy;
    }

    /**
     * Calculates the maximum payload capacity of an image in bytes.
     */
    public int getCapacityBytes(BufferedImage image) {
        return (image.getWidth() * image.getHeight() * 3) / 8;
    }
}