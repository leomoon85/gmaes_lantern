package com.lantern.dispatch;

import android.graphics.Bitmap;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.EnumMap;
import java.util.Map;

/** Pure-Java QR code generator and camera frame decoder for Fareforge Convoy multiplayer & backups. */
final class QrCodeHelper {
  private static final String PREFIX = "FFLINK|";

  private QrCodeHelper() {}

  static String packRoomPayload(String roomCode, String envelope) {
    return PREFIX + roomCode + "|" + envelope;
  }

  static String[] unpackRoomPayload(String raw) {
    if (raw == null) return null;
    String trimmed = raw.trim();
    if (!trimmed.startsWith(PREFIX)) return null;
    String rest = trimmed.substring(PREFIX.length());
    int sep = rest.indexOf('|');
    if (sep <= 0 || sep >= rest.length() - 1) return null;
    return new String[] {rest.substring(0, sep).trim(), rest.substring(sep + 1).trim()};
  }

  static Bitmap encodeQr(String payload, int sizePx) throws Exception {
    Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
    hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    hints.put(EncodeHintType.MARGIN, 2);
    BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
    int w = matrix.getWidth();
    int h = matrix.getHeight();
    int[] pixels = new int[w * h];
    for (int y = 0; y < h; y++) {
      int offset = y * w;
      for (int x = 0; x < w; x++) {
        pixels[offset + x] = matrix.get(x, y) ? 0xFF0A1422 : 0xFFFFFFFF;
      }
    }
    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    bmp.setPixels(pixels, 0, w, 0, 0, w, h);
    return bmp;
  }

  static String decodeYuv(byte[] yPlane, int width, int height) {
    if (yPlane == null || width <= 0 || height <= 0) return null;
    try {
      PlanarYUVLuminanceSource source =
          new PlanarYUVLuminanceSource(yPlane, width, height, 0, 0, width, height, false);
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
      Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
      hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
      Result result = new QRCodeReader().decode(bitmap, hints);
      return result != null ? result.getText() : null;
    } catch (Exception ignored) {
      return null;
    }
  }
}
