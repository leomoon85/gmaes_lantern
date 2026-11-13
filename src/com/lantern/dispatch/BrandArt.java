package com.lantern.dispatch;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Original code-drawn identity, navigation icons, Tropico-inspired medallions, and city artwork. */
final class BrandArt extends View {
  private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Shader backdrop =
      new LinearGradient(
          0,
          0,
          420,
          210,
          new int[] {0xff1c2d48, 0xff132238, 0xff0a1322},
          new float[] {0f, 0.58f, 1f},
          Shader.TileMode.CLAMP);
  private final Shader sunGlow =
      new RadialGradient(
          336, 48, 95, 0x55ffd27d, 0x00ffd27d, Shader.TileMode.CLAMP);
  private final Path street = new Path();
  private final DashPathEffect laneDashes = new DashPathEffect(new float[] {6, 7}, 0);
  private static final float[][] NODES = {{52, 145}, {177, 164}, {281, 125}, {374, 103}};

  BrandArt(Context context) {
    super(context);
    setContentDescription("Fareforge city network illustration");
    street.moveTo(-10, 175);
    street.cubicTo(110, 90, 170, 220, 260, 142);
    street.cubicTo(330, 85, 365, 140, 440, 67);
  }

  @Override
  protected void onDraw(Canvas c) {
    float sx = getWidth() / 420f, sy = getHeight() / 210f;
    c.save();
    c.scale(sx, sy);
    p.setStyle(Paint.Style.FILL);
    p.setShader(backdrop);
    c.drawRect(0, 0, 420, 210, p);
    p.setShader(sunGlow);
    c.drawCircle(336, 48, 95, p);
    p.setShader(null);

    // Distant harbor & tropical coast band
    p.setColor(0xff102c3f);
    c.drawRect(0, 152, 420, 210, p);
    p.setColor(0x3373d6cf);
    c.drawRect(0, 152, 420, 155, p);

    // Sunlit skyline silhouettes with brass-lit windows
    for (int i = 0; i < 14; i++) {
      float x = i * 32 - 10, top = 34 + (i * 37 % 72);
      p.setColor(i % 2 == 0 ? 0xff192c46 : 0xff213654);
      c.drawRoundRect(x, top, x + 25, 162, 3, 3, p);
      p.setColor(0x55ffd27a);
      c.drawRect(x + 2, top, x + 23, top + 2, p);
      for (int y = 0; y < 5; y++) {
        float wy = top + 10 + y * 16;
        if (wy < 150) {
          p.setColor((i + y) % 3 == 0 ? 0xbbffd485 : 0xff314b68);
          c.drawRect(x + 5, wy, x + 10, wy + 6, p);
          c.drawRect(x + 14, wy, x + 19, wy + 6, p);
        }
      }
    }

    // Sculpted highway corridor & brass rail trim
    p.setStrokeCap(Paint.Cap.ROUND);
    c.drawPath(street, stroke(0x55000000, 28));
    c.drawPath(street, stroke(0xff38526e, 24));
    c.drawPath(street, stroke(0xff142336, 18));
    p.setPathEffect(laneDashes);
    c.drawPath(street, stroke(0xfff5c472, 2.1f));
    p.setPathEffect(null);

    // Ornate station medallions along the route
    p.setStyle(Paint.Style.FILL);
    for (int i = 0; i < NODES.length; i++) {
      float[] node = NODES[i];
      p.setColor(0x33ffd184);
      c.drawCircle(node[0], node[1], 18, p);
      p.setColor(i == 2 ? 0xff73d6cf : 0xfff5c071);
      c.drawCircle(node[0], node[1], 8.5f, p);
      p.setColor(0xff101b2c);
      c.drawCircle(node[0], node[1], 4.2f, p);
      p.setColor(0xfffff2cf);
      c.drawCircle(node[0], node[1], 1.8f, p);
    }

    // Hero Tycoon Coach on the avenue
    c.save();
    c.translate(229, 157);
    c.rotate(-24);
    p.setColor(0x66000000);
    c.drawRoundRect(-23, -9, 23, 13, 6, 6, p);
    p.setColor(0xfff4bf6f);
    c.drawRoundRect(-22, -11, 22, 11, 5, 5, p);
    p.setColor(0xff172435);
    c.drawRoundRect(-9, -8, 9, 8, 2.5f, 2.5f, p);
    p.setColor(0xff73d6cf);
    c.drawRoundRect(-3, -3, 3, 3, 1.5f, 1.5f, p);
    p.setColor(0xfffff4cc);
    c.drawRect(17, -8, 21, -3, p);
    c.drawRect(17, 3, 21, 8, p);
    c.restore();

    // Sun & compass rose seal in sky
    p.setColor(0xfff5c472);
    c.drawCircle(336, 48, 7, p);
    c.drawCircle(336, 48, 15, stroke(0x88f5c472, 1.4f));
    c.restore();
  }

  static View meterRow(
      Context ctx, String label, int percent, int fillColor, String rightText, float density) {
    int clamped = Math.max(0, Math.min(100, percent));
    LinearLayout wrap = new LinearLayout(ctx);
    wrap.setOrientation(LinearLayout.VERTICAL);
    LinearLayout.LayoutParams wrapLp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    wrapLp.topMargin = Math.round(5 * density);
    wrap.setLayoutParams(wrapLp);

    LinearLayout top = new LinearLayout(ctx);
    top.setOrientation(LinearLayout.HORIZONTAL);
    top.setGravity(Gravity.CENTER_VERTICAL);
    TextView left = new TextView(ctx);
    left.setText(label);
    left.setTextColor(0xffb8c7d9);
    left.setTextSize(12.5f);
    left.setTypeface(Typeface.DEFAULT_BOLD);
    top.addView(
        left,
        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

    TextView right = new TextView(ctx);
    right.setText(rightText != null ? rightText : (clamped + "%"));
    right.setTextColor(fillColor);
    right.setTextSize(12.5f);
    right.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
    top.addView(right);
    wrap.addView(top);

    LinearLayout track = new LinearLayout(ctx);
    track.setOrientation(LinearLayout.HORIZONTAL);
    GradientDrawable trackBg = new GradientDrawable();
    trackBg.setColor(0xff0b1320);
    trackBg.setCornerRadius(5 * density);
    trackBg.setStroke(Math.max(1, Math.round(density * 0.8f)), 0xff273a52);
    track.setBackground(trackBg);
    LinearLayout.LayoutParams trackLp =
        new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, Math.round(9 * density));
    trackLp.topMargin = Math.round(2 * density);
    track.setLayoutParams(trackLp);

    View fill = new View(ctx);
    GradientDrawable fillBg = new GradientDrawable();
    fillBg.setColor(fillColor);
    fillBg.setCornerRadius(5 * density);
    fill.setBackground(fillBg);
    track.addView(
        fill,
        new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, Math.max(2, clamped)));

    View rest = new View(ctx);
    track.addView(
        rest,
        new LinearLayout.LayoutParams(
            0, LinearLayout.LayoutParams.MATCH_PARENT, Math.max(0, 100 - clamped)));

    wrap.addView(track);
    return wrap;
  }

  static Drawable crestDrawable(final int fillColor, final int ringColor, final float density) {
    return new Drawable() {
      final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

      @Override
      public int getIntrinsicWidth() {
        return Math.round(32 * density);
      }

      @Override
      public int getIntrinsicHeight() {
        return Math.round(32 * density);
      }

      @Override
      public void draw(Canvas canvas) {
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float r = Math.min(b.width(), b.height()) * 0.46f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fillColor);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.5f, 1.8f * density));
        paint.setColor(ringColor);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setStrokeWidth(Math.max(1f, 0.9f * density));
        paint.setColor(0x66ffd996);
        canvas.drawCircle(cx, cy, r * 0.76f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ringColor);
        Path star = new Path();
        star.moveTo(cx, cy - r * 0.52f);
        star.lineTo(cx + r * 0.16f, cy - r * 0.16f);
        star.lineTo(cx + r * 0.52f, cy);
        star.lineTo(cx + r * 0.16f, cy + r * 0.16f);
        star.lineTo(cx, cy + r * 0.52f);
        star.lineTo(cx - r * 0.16f, cy + r * 0.16f);
        star.lineTo(cx - r * 0.52f, cy);
        star.lineTo(cx - r * 0.16f, cy - r * 0.16f);
        star.close();
        canvas.drawPath(star, paint);
      }

      @Override
      public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
      }

      @Override
      public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
      }

      @Override
      public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
      }
    };
  }

  /** Illustrated character guide portrait (Chief Dispatcher Maya) for the step-by-step tutorial. */
  static Drawable advisorPortraitDrawable(final float density) {
    return new Drawable() {
      final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

      @Override
      public int getIntrinsicWidth() {
        return Math.round(48 * density);
      }

      @Override
      public int getIntrinsicHeight() {
        return Math.round(48 * density);
      }

      @Override
      public void draw(Canvas canvas) {
        Rect b = getBounds();
        float cx = b.exactCenterX(), cy = b.exactCenterY();
        float r = Math.min(b.width(), b.height()) * 0.46f;
        // Circular badge backdrop
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xff17344f);
        canvas.drawCircle(cx, cy, r, paint);
        // Uniform shoulders
        paint.setColor(0xff264e73);
        canvas.drawArc(
            cx - r * 0.78f, cy + r * 0.28f, cx + r * 0.78f, cy + r * 1.15f, 180, 180, true, paint);
        // Brass tie / collar pin
        paint.setColor(0xffedbb73);
        canvas.drawCircle(cx, cy + r * 0.62f, r * 0.11f, paint);
        // Hair behind face
        paint.setColor(0xff2c1f24);
        canvas.drawCircle(cx, cy - r * 0.04f, r * 0.48f, paint);
        // Face
        paint.setColor(0xffebb88b);
        canvas.drawCircle(cx, cy + r * 0.02f, r * 0.36f, paint);
        // Dispatcher cap & gold visor
        paint.setColor(0xff1d3d5c);
        canvas.drawArc(
            cx - r * 0.40f, cy - r * 0.56f, cx + r * 0.40f, cy + r * 0.05f, 180, 180, true, paint);
        paint.setColor(0xffedbb73);
        canvas.drawRoundRect(
            cx - r * 0.38f, cy - r * 0.24f, cx + r * 0.38f, cy - r * 0.14f, r * 0.05f, r * 0.05f, paint);
        // Cap crest dot
        canvas.drawCircle(cx, cy - r * 0.36f, r * 0.07f, paint);
        // Eyes & friendly smile
        paint.setColor(0xff101d30);
        canvas.drawCircle(cx - r * 0.12f, cy - r * 0.02f, r * 0.05f, paint);
        canvas.drawCircle(cx + r * 0.12f, cy - r * 0.02f, r * 0.05f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1.2f, 1.3f * density));
        canvas.drawArc(
            cx - r * 0.13f, cy + r * 0.06f, cx + r * 0.13f, cy + r * 0.22f, 15, 150, false, paint);
        // Radio headset boom mic
        paint.setColor(0xff77d7c8);
        paint.setStrokeWidth(Math.max(1.4f, 1.5f * density));
        canvas.drawLine(cx - r * 0.37f, cy + r * 0.04f, cx - r * 0.12f, cy + r * 0.22f, paint);
        // Gold portrait frame ring
        paint.setColor(0xffedbb73);
        paint.setStrokeWidth(Math.max(1.6f, 1.9f * density));
        canvas.drawCircle(cx, cy, r, paint);
      }

      @Override
      public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
      }

      @Override
      public void setColorFilter(ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
      }

      @Override
      public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
      }
    };
  }

  private Paint stroke(int color, float width) {
    p.setShader(null);
    p.setColor(color);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(width);
    return p;
  }

  static Drawable icon(int type, int color, float density) {
    return new NavIcon(type, color, density);
  }

  private static final class NavIcon extends Drawable {
    final int type, color;
    final float density;
    final Paint p = new Paint(3);

    NavIcon(int type, int color, float density) {
      this.type = type;
      this.color = color;
      this.density = density;
    }

    @Override
    public int getIntrinsicWidth() {
      return Math.round(22 * density);
    }

    @Override
    public int getIntrinsicHeight() {
      return Math.round(22 * density);
    }

    @Override
    public void draw(Canvas c) {
      c.save();
      c.translate(getBounds().left, getBounds().top);
      c.scale(getBounds().width() / 24f, getBounds().height() / 24f);
      p.setColor(color);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(1.7f);
      p.setStrokeCap(Paint.Cap.ROUND);
      p.setStrokeJoin(Paint.Join.ROUND);
      switch (type) {
        case 0:
          c.drawRect(3, 4, 9, 10, p);
          c.drawRect(14, 3, 21, 10, p);
          c.drawRect(3, 15, 9, 21, p);
          c.drawRect(14, 15, 21, 21, p);
          break;
        case 1:
          c.drawRoundRect(3, 9, 21, 18, 3, 3, p);
          c.drawLine(6, 9, 8, 4, p);
          c.drawLine(8, 4, 16, 4, p);
          c.drawLine(16, 4, 18, 9, p);
          c.drawLine(7, 13, 9, 13, p);
          c.drawLine(15, 13, 17, 13, p);
          c.drawLine(6, 18, 6, 21, p);
          c.drawLine(18, 18, 18, 21, p);
          break;
        case 2:
          c.drawCircle(5, 18, 3, p);
          c.drawCircle(19, 18, 3, p);
          c.drawCircle(12, 5, 3, p);
          c.drawLine(7, 15, 11, 8, p);
          c.drawLine(17, 15, 13, 8, p);
          c.drawLine(8, 18, 16, 18, p);
          break;
        case 3:
          c.drawCircle(9, 7, 3, p);
          c.drawArc(3, 12, 15, 24, 180, 180, false, p);
          c.drawCircle(18, 8, 2, p);
          c.drawArc(14, 13, 24, 23, 210, 135, false, p);
          break;
        case 4:
          // Ledger book / balance sheet icon
          c.drawRoundRect(4, 3, 20, 21, 2, 2, p);
          c.drawLine(8, 3, 8, 21, p);
          c.drawLine(11, 8, 17, 8, p);
          c.drawLine(11, 12, 17, 12, p);
          c.drawLine(11, 16, 15, 16, p);
          break;
        case 5:
          c.drawLine(4, 3, 4, 21, p);
          c.drawLine(4, 21, 22, 21, p);
          Path path = new Path();
          path.moveTo(7, 16);
          path.lineTo(12, 10);
          path.lineTo(16, 13);
          path.lineTo(21, 5);
          c.drawPath(path, p);
          break;
        default:
          c.drawRoundRect(3, 3, 19, 21, 2, 2, p);
          c.drawRect(6, 6, 11, 11, p);
          c.drawLine(14, 6, 16, 6, p);
          c.drawLine(14, 10, 16, 10, p);
          c.drawLine(6, 15, 16, 15, p);
          c.drawLine(6, 18, 14, 18, p);
          break;
      }
      c.restore();
    }

    @Override
    public void setAlpha(int alpha) {
      p.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
      p.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
      return PixelFormat.TRANSLUCENT;
    }
  }
}
