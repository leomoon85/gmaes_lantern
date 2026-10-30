package com.lantern.dispatch;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Original code-drawn identity, navigation icons, and city artwork. */
final class BrandArt extends View {
  private final Paint p = new Paint(3);
  private final Shader backdrop =
      new LinearGradient(0, 0, 420, 210, 0xff111d34, 0xff080f1d, Shader.TileMode.CLAMP);
  private final Path street = new Path();
  private final DashPathEffect laneDashes = new DashPathEffect(new float[] {5, 7}, 0);
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
    p.setShader(null);
    p.setColor(0xff192b43);
    for (int i = 0; i < 14; i++) {
      float x = i * 34 - 15, top = 30 + (i * 37 % 80);
      c.drawRoundRect(x, top, x + 26, 190, 3, 3, p);
    }
    p.setColor(0xff2b425b);
    for (int i = 0; i < 14; i++)
      for (int y = 0; y < 5; y++) {
        float x = i * 34 - 10, top = 45 + (i * 37 % 80) + y * 17;
        if (top < 180) c.drawRect(x, top, x + 5, top + 6, p);
      }
    p.setColor(0xff101a2b);
    p.setStrokeWidth(24);
    p.setStrokeCap(Paint.Cap.ROUND);
    c.drawPath(street, stroke(0xff263a52, 24));
    c.drawPath(street, stroke(0xff111e30, 18));
    p.setPathEffect(laneDashes);
    c.drawPath(street, stroke(0xffe9b46b, 1.8f));
    p.setPathEffect(null);
    p.setStyle(Paint.Style.FILL);
    for (float[] node : NODES) {
      p.setColor(0x22ffd184);
      c.drawCircle(node[0], node[1], 17, p);
      p.setColor(0xfff5c071);
      c.drawCircle(node[0], node[1], 7, p);
      p.setColor(0xff101b2c);
      c.drawCircle(node[0], node[1], 3, p);
    }
    c.save();
    c.translate(229, 157);
    c.rotate(-24);
    p.setColor(0xfff3bd70);
    c.drawRoundRect(-22, -11, 22, 11, 5, 5, p);
    p.setColor(0xff172435);
    c.drawRoundRect(-8, -8, 9, 8, 2, 2, p);
    p.setColor(0xfffff4cc);
    c.drawRect(17, -8, 20, -3, p);
    c.drawRect(17, 3, 20, 8, p);
    c.restore();
    p.setColor(0xff75d5cb);
    c.drawCircle(330, 49, 5, p);
    p.setColor(0xff31495d);
    c.drawCircle(330, 49, 13, stroke(0xff31495d, 1));
    c.restore();
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
