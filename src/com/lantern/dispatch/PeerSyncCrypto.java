package com.lantern.dispatch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Fareforge Convoy serverless E2EE cryptographic envelope.
 *
 * Utilizes 256-bit AES-GCM authenticated encryption with 12-byte random IVs
 * and SHA-256 key derivation, allowing zero-server encrypted cloud backups
 * and multiplayer room state sync over decentralized relays or direct passcodes.
 */
public final class PeerSyncCrypto {
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int TAG_LENGTH_BITS = 128;
  private static final int IV_LENGTH_BYTES = 12;
  private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
  private static final SecureRandom RANDOM = new SecureRandom();

  private PeerSyncCrypto() {}

  /** Derives a 256-bit AES key from a passphrase or room code using SHA-256. */
  public static byte[] deriveKey(String secret) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return md.digest(secret.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new RuntimeException("SHA-256 unavailable", e);
    }
  }

  /** Generates a fresh random 12-character alphanumeric room key (e.g. FORGE-8A2F9C). */
  public static String generateRoomCode() {
    byte[] randomBytes = new byte[4];
    RANDOM.nextBytes(randomBytes);
    return "FORGE-" + toHex(randomBytes).toUpperCase();
  }

  /**
   * Encrypts plaintext bytes using AES-GCM with a random IV.
   * Format: [12 bytes IV] + [Ciphertext + 16 bytes GCM Auth Tag]
   */
  public static byte[] encrypt(byte[] plaintext, byte[] key) throws Exception {
    byte[] iv = new byte[IV_LENGTH_BYTES];
    RANDOM.nextBytes(iv);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

    byte[] ciphertext = cipher.doFinal(plaintext);
    byte[] combined = new byte[iv.length + ciphertext.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
    return combined;
  }

  /** Decrypts and authenticates an AES-GCM envelope produced by encrypt(). */
  public static byte[] decrypt(byte[] combined, byte[] key) throws Exception {
    if (combined == null || combined.length < IV_LENGTH_BYTES + 16) {
      throw new IllegalArgumentException("Invalid encrypted payload size");
    }
    byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
    byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);

    Cipher cipher = Cipher.getInstance(ALGORITHM);
    SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
    GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_BITS, iv);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

    return cipher.doFinal(ciphertext);
  }

  public static String toHex(byte[] bytes) {
    char[] hexChars = new char[bytes.length * 2];
    for (int j = 0; j < bytes.length; j++) {
      int v = bytes[j] & 0xFF;
      hexChars[j * 2] = HEX_CHARS[v >>> 4];
      hexChars[j * 2 + 1] = HEX_CHARS[v & 0x0F];
    }
    return new String(hexChars);
  }

  public static byte[] fromHex(String s) {
    if (s == null) return new byte[0];
    String clean = s.trim().replaceAll("[^0-9a-fA-F]", "");
    int len = clean.length();
    if (len % 2 != 0) {
      throw new IllegalArgumentException("Hex string must have even length");
    }
    byte[] data = new byte[len / 2];
    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)
              ((Character.digit(clean.charAt(i), 16) << 4)
                  + Character.digit(clean.charAt(i + 1), 16));
    }
    return data;
  }

  /** Formats an encrypted payload as a compact portable armor block. */
  public static String toArmor(String type, byte[] payload) {
    return "-----BEGIN FAREFORGE " + type + "-----\n" + toHex(payload) + "\n-----END FAREFORGE " + type + "-----";
  }

  /** Parses an armored block back to raw bytes. */
  public static byte[] fromArmor(String text) {
    if (text == null) return new byte[0];
    String clean = text.replaceAll("-----BEGIN [A-Z0-9 ]+-----", "")
                       .replaceAll("-----END [A-Z0-9 ]+-----", "")
                       .replaceAll("\\s+", "");
    return fromHex(clean);
  }
}
