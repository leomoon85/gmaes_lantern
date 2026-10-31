package com.lantern.dispatch;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** Native, offline vector city. The road network is schematic, not Google road data. */
final class CityMapView extends View {
  interface Listener {
    void hubSelected(int index);
  }

  private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF rect = new RectF();
  private final DashPathEffect dashEffect = new DashPathEffect(new float[] {6f, 6f}, 0f);
  private final GestureDetector gestures;
  private final ScaleGestureDetector pinch;
  private Game game;
  private Listener listener;
  private float zoom = 1, panX = 0, panY = 0, unit = 1, left = 0, top = 0;
  private boolean night = true;
  private long lastRides = -1;
  private long lastRevenue = 0;
  private String popupText = null;
  private int popupCell = -1;
  private long popupUntilMs = 0;

  CityMapView(Context context) {
    super(context);
    setContentDescription(
        "City network. Pan or pinch to explore. Open Hubs for accessible station controls.");
    setFocusable(true);
    pinch =
        new ScaleGestureDetector(
            context,
            new ScaleGestureDetector.SimpleOnScaleGestureListener() {
              @Override
              public boolean onScale(ScaleGestureDetector detector) {
                zoom = Math.max(.7f, Math.min(3f, zoom * detector.getScaleFactor()));
                invalidate();
                return true;
              }
            });
    gestures =
        new GestureDetector(
            context,
            new GestureDetector.SimpleOnGestureListener() {
              @Override
              public boolean onDown(MotionEvent event) {
                return true;
              }

              @Override
              public boolean onScroll(MotionEvent a, MotionEvent b, float dx, float dy) {
                if (!pinch.isInProgress()) {
                  panX = Math.max(-getWidth(), Math.min(getWidth(), panX - dx));
                  panY = Math.max(-getHeight(), Math.min(getHeight(), panY - dy));
                  invalidate();
                }
                return true;
              }

              @Override
              public boolean onDoubleTap(MotionEvent event) {
                resetCamera();
                return true;
              }

              @Override
              public boolean onSingleTapConfirmed(MotionEvent event) {
                performClick();
                if (game == null) return true;
                float best = 44 * getResources().getDisplayMetrics().density;
                int selected = -1;
                for (int i = 0; i < game.districts.size(); i++) {
                  Game.District d = game.districts.get(i);
                  float dx = event.getX() - screenX(d.cell()),
                      dy = event.getY() - screenY(d.cell());
                  float distance = (float) Math.hypot(dx, dy);
                  if (distance < best) {
                    best = distance;
                    selected = i;
                  }
                }
                if (selected >= 0 && listener != null) listener.hubSelected(selected);
                return true;
              }
            });
  }

  void bind(Game value, Listener callback) {
    game = value;
    listener = callback;
    invalidate();
  }

  void setNight(boolean value) {
    night = value;
    invalidate();
  }

  void resetCamera() {
    zoom = 1;
    panX = panY = 0;
    invalidate();
  }

  @Override
  public boolean performClick() {
    super.performClick();
    return true;
  }

  @android.annotation.SuppressLint(
      "ClickableViewAccessibility") // GestureDetector calls performClick only for confirmed taps;
  // hubs also have native controls.
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    pinch.onTouchEvent(event);
    gestures.onTouchEvent(event);
    return true;
  }

  private float cellX(int cell) {
    return (cell % Game.W + .5f) * unit;
  }

  private float cellY(int cell) {
    return (cell / Game.W + .5f) * unit;
  }

  private float screenX(int cell) {
    return left + panX + (cellX(cell) - Game.W * unit / 2) * zoom + Game.W * unit / 2;
  }

  private float screenY(int cell) {
    return top + panY + (cellY(cell) - Game.H * unit / 2) * zoom + Game.H * unit / 2;
  }

  private void fill(int color) {
    paint.setColor(color);
    paint.setStyle(Paint.Style.FILL);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (game == null) return;
    if (lastRides >= 0 && game.rides > lastRides) {
      long delta = Math.max(15, game.revenue - lastRevenue);
      popupText = "+" + Game.money(delta);
      popupUntilMs = android.os.SystemClock.uptimeMillis() + 2200L;
      for (Game.Taxi t : game.taxis)
        if (t.status == Game.CHARGING) {
          popupCell = t.cell;
          break;
        }
    }
    lastRides = game.rides;
    lastRevenue = game.revenue;

    int ground = night ? 0xff091322 : 0xffe4decb,
        block = night ? 0xff18283e : 0xffd2cab6,
        road = night ? 0xff283a50 : 0xff8594a3,
        roadTrim = night ? 0xff3f5672 : 0xffa7b4c0;
    canvas.drawColor(ground);
    float density = getResources().getDisplayMetrics().density;
    unit = Math.min(getWidth() / (float) Game.W, getHeight() / (float) Game.H) * .94f;
    left = (getWidth() - Game.W * unit) / 2;
    top = (getHeight() - Game.H * unit) / 2;
    canvas.save();
    canvas.translate(left + panX + Game.W * unit / 2, top + panY + Game.H * unit / 2);
    canvas.scale(zoom, zoom);
    canvas.translate(-Game.W * unit / 2, -Game.H * unit / 2);

    // Subtle game board frame
    fill(night ? 0xff0d1b2e : 0xffdcd4bf);
    rect.set(-unit * 0.4f, -unit * 0.4f, (Game.W + 0.4f) * unit, (Game.H + 0.4f) * unit);
    canvas.drawRoundRect(rect, unit * 0.6f, unit * 0.6f, paint);

    // 1. Water, parks, and 3D-extruded urban blocks
    for (int y = 0; y < Game.H; y++)
      for (int x = 0; x < Game.W; x++) {
        if (Game.road(x, y)) continue;
        boolean isWater = Game.water(x, y);
        boolean isPark = !isWater && Game.park(x, y);
        if (isWater) {
          fill(night ? 0xff11324d : 0xff5fa8bc);
          rect.set(
              x * unit + unit * .06f,
              y * unit + unit * .06f,
              (x + 1) * unit - unit * .06f,
              (y + 1) * unit - unit * .06f);
          canvas.drawRoundRect(rect, unit * .18f, unit * .18f, paint);
          paint.setColor(night ? 0x4473d6cf : 0x66ffffff);
          paint.setStyle(Paint.Style.STROKE);
          paint.setStrokeWidth(unit * .06f);
          canvas.drawLine(
              x * unit + unit * .22f,
              y * unit + unit * .52f,
              x * unit + unit * .78f,
              y * unit + unit * .52f,
              paint);
        } else if (isPark) {
          fill(night ? 0xff193b39 : 0xff8cb87a);
          rect.set(
              x * unit + unit * .08f,
              y * unit + unit * .08f,
              (x + 1) * unit - unit * .08f,
              (y + 1) * unit - unit * .08f);
          canvas.drawRoundRect(rect, unit * .20f, unit * .20f, paint);
          fill(night ? 0xff285952 : 0xff629654);
          canvas.drawCircle(x * unit + unit * .36f, y * unit + unit * .40f, unit * .20f, paint);
          canvas.drawCircle(x * unit + unit * .66f, y * unit + unit * .62f, unit * .17f, paint);
        } else {
          // Drop shadow for 3D city block
          fill(night ? 0x6602060d : 0x335a5244);
          rect.set(
              x * unit + unit * .14f,
              y * unit + unit * .16f,
              (x + 1) * unit - unit * .06f,
              (y + 1) * unit - unit * .04f);
          canvas.drawRoundRect(rect, unit * .15f, unit * .15f, paint);

          int bColor =
              (x * 7 + y * 13) % 4 == 0
                  ? (night ? 0xff1e314a : 0xffc6bea9)
                  : block;
          fill(bColor);
          rect.set(
              x * unit + unit * .10f,
              y * unit + unit * .10f,
              (x + 1) * unit - unit * .10f,
              (y + 1) * unit - unit * .10f);
          canvas.drawRoundRect(rect, unit * .15f, unit * .15f, paint);

          if ((x + y) % 2 == 0) {
            fill(night ? 0xff263d5a : 0xffb8af98);
            canvas.drawRoundRect(
                x * unit + .22f * unit,
                y * unit + .20f * unit,
                (x + .78f) * unit,
                (y + .78f) * unit,
                unit * .08f,
                unit * .08f,
                paint);
            if (night && (x * 3 + y * 5) % 4 == 0) {
              fill(0xbbffd27d);
              canvas.drawRect(
                  x * unit + .32f * unit,
                  y * unit + .32f * unit,
                  x * unit + .44f * unit,
                  y * unit + .44f * unit,
                  paint);
              canvas.drawRect(
                  x * unit + .54f * unit,
                  y * unit + .52f * unit,
                  x * unit + .66f * unit,
                  y * unit + .64f * unit,
                  paint);
            }
          }
        }
      }

    // 2. Multi-lane avenues with curbs & dashed centerlines
    paint.setStrokeCap(Paint.Cap.ROUND);
    for (int x = 0; x < Game.W; x += 5) {
      float cx = (x + .5f) * unit;
      boolean arterial = x == 15;
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(roadTrim);
      paint.setStrokeWidth(arterial ? unit * .62f : unit * .36f);
      canvas.drawLine(cx, unit * .5f, cx, (Game.H - .5f) * unit, paint);
      paint.setColor(road);
      paint.setStrokeWidth(arterial ? unit * .50f : unit * .28f);
      canvas.drawLine(cx, unit * .5f, cx, (Game.H - .5f) * unit, paint);
    }
    for (int y = 0; y < Game.H; y += 4) {
      float cy = (y + .5f) * unit;
      boolean arterial = y == 4 || y == 16;
      paint.setStyle(Paint.Style.STROKE);
      paint.setColor(roadTrim);
      paint.setStrokeWidth(arterial ? unit * .62f : unit * .36f);
      canvas.drawLine(unit * .5f, cy, (Game.W - .5f) * unit, cy, paint);
      paint.setColor(road);
      paint.setStrokeWidth(arterial ? unit * .50f : unit * .28f);
      canvas.drawLine(unit * .5f, cy, (Game.W - .5f) * unit, cy, paint);
    }

    // Arterial lane dashes
    paint.setPathEffect(dashEffect);
    paint.setColor(night ? 0x66f3bd70 : 0x88fff3d1);
    paint.setStrokeWidth(unit * .045f);
    canvas.drawLine(15.5f * unit, unit * .5f, 15.5f * unit, (Game.H - .5f) * unit, paint);
    canvas.drawLine(unit * .5f, 4.5f * unit, (Game.W - .5f) * unit, 4.5f * unit, paint);
    canvas.drawLine(unit * .5f, 16.5f * unit, (Game.W - .5f) * unit, 16.5f * unit, paint);
    paint.setPathEffect(null);

    // 3. Built service corridors (Tier 1 Standard, Tier 2 Express, Tier 3 Maglev Gold)
    for (int cell = 0; cell < game.corridorTier.length; cell++)
      if (game.corridorTier[cell] > 0) {
        int tier = game.corridorTier[cell];
        int cColor = tier == 3 ? 0xfff5c472 : tier == 2 ? 0xff73d6cf : 0xff62a885;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0x44000000);
        paint.setStrokeWidth(unit * (tier == 1 ? .20f : .30f));
        if (cell % Game.W < Game.W - 1 && game.corridorTier[cell + 1] > 0)
          canvas.drawLine(cellX(cell), cellY(cell), cellX(cell + 1), cellY(cell + 1), paint);
        if (cell / Game.W < Game.H - 1 && game.corridorTier[cell + Game.W] > 0)
          canvas.drawLine(
              cellX(cell), cellY(cell), cellX(cell + Game.W), cellY(cell + Game.W), paint);

        paint.setColor(cColor);
        paint.setStrokeWidth(unit * (tier == 1 ? .14f : .22f));
        if (cell % Game.W < Game.W - 1 && game.corridorTier[cell + 1] > 0)
          canvas.drawLine(cellX(cell), cellY(cell), cellX(cell + 1), cellY(cell + 1), paint);
        if (cell / Game.W < Game.H - 1 && game.corridorTier[cell + Game.W] > 0)
          canvas.drawLine(
              cellX(cell), cellY(cell), cellX(cell + Game.W), cellY(cell + Game.W), paint);
      }

    // 4. Active dispatch route traces
    for (Game.Taxi taxi : game.taxis) {
      if ((taxi.status == Game.PICKUP || taxi.status == Game.RIDING)
          && taxi.path != null
          && taxi.at < taxi.path.length - 1) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(taxi.status == Game.RIDING ? 0x887ee2b8 : 0x88f5c472);
        paint.setStrokeWidth(unit * .10f);
        for (int s = Math.max(0, taxi.at); s < taxi.path.length - 1; s++) {
          canvas.drawLine(
              cellX(taxi.path[s]),
              cellY(taxi.path[s]),
              cellX(taxi.path[s + 1]),
              cellY(taxi.path[s + 1]),
              paint);
        }
      }
    }

    // 5. Rival & Player fleets
    for (Game.Company company : game.companies)
      if (!company.merged)
        vehicle(
            canvas,
            company.cell,
            company.at,
            company.path,
            company.progress,
            company.color,
            unit * .29f,
            0);
    for (Game.Taxi taxi : game.taxis)
      vehicle(
          canvas,
          taxi.cell,
          taxi.at,
          taxi.path,
          taxi.progress,
          taxi.broken ? 0xffed927d : taxi.status == Game.RIDING ? 0xff7ee2b8 : 0xfff5c472,
          unit * .46f,
          taxi.id);

    // 6. Sculpted Station Hub Medallions
    int[] waitingByHub = new int[game.districts.size()];
    for (Game.Ride r : game.requests)
      if (r.from >= 0 && r.from < waitingByHub.length) waitingByHub[r.from]++;

    for (int i = 0; i < game.districts.size(); i++) {
      Game.District hub = game.districts.get(i);
      float x = cellX(hub.cell()), y = cellY(hub.cell());
      if (waitingByHub[i] > 0) {
        fill(0x38f5c472);
        canvas.drawCircle(x, y, unit * 1.25f, paint);
      }
      // Drop shadow
      fill(0x77000000);
      canvas.drawCircle(x, y + unit * .08f, unit * .96f, paint);
      // Medallion body
      fill(night ? 0xff0d1a2d : 0xfff6f1e1);
      canvas.drawCircle(x, y, unit * .94f, paint);
      // Outer brass/hub ring
      paint.setColor(hub.open ? hub.color : 0xff6e7f8d);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(unit * .19f);
      canvas.drawCircle(x, y, unit * .76f, paint);
      // Inner trim ring
      paint.setColor(hub.open ? 0x88ffe3a8 : 0x4491a79b);
      paint.setStrokeWidth(unit * .05f);
      canvas.drawCircle(x, y, unit * .56f, paint);

      fill(hub.open ? 0xfffff3d6 : 0xff91a79b);
      paint.setTextSize(unit * .64f);
      paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
      paint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(hub.open ? String.valueOf(hub.level) : "+", x, y + unit * .23f, paint);
    }
    canvas.restore();

    // 7. Screen-space Tropico-style station nameplates & waiting request pills
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
    paint.setTextSize(10.5f * density);
    for (int i = 0; i < game.districts.size(); i++) {
      Game.District hub = game.districts.get(i);
      String stars =
          hub.open
              ? (hub.level == 3 ? " ★★★" : hub.level == 2 ? " ★★" : " ★")
              : " [LOCKED]";
      String label = hub.name + stars;
      float x = screenX(hub.cell()), y = screenY(hub.cell()) + unit * zoom + 14 * density;
      float width = paint.measureText(label) + 16 * density;
      x = Math.max(width / 2 + 4, Math.min(getWidth() - width / 2 - 4, x));

      fill(night ? 0xf0101e33 : 0xf0f5efe0);
      rect.set(x - width / 2, y - 13 * density, x + width / 2, y + 5 * density);
      canvas.drawRoundRect(rect, 7 * density, 7 * density, paint);

      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(Math.max(1f, density));
      paint.setColor(hub.open ? hub.color : 0xff5a6d7c);
      canvas.drawRoundRect(rect, 7 * density, 7 * density, paint);

      fill(night ? 0xfff5ebd6 : 0xff1f3229);
      canvas.drawText(label, x, y - 0.5f * density, paint);

      if (waitingByHub[i] > 0) {
        String badge = waitingByHub[i] + "FARE";
        float bx = screenX(hub.cell()) + unit * zoom * 0.72f;
        float by = screenY(hub.cell()) - unit * zoom * 0.65f;
        float bw = paint.measureText(badge) + 10 * density;
        fill(0xfff5c472);
        rect.set(bx - bw / 2, by - 9 * density, bx + bw / 2, by + 4 * density);
        canvas.drawRoundRect(rect, 6 * density, 6 * density, paint);
        fill(0xff101c2e);
        paint.setTextSize(8.5f * density);
        canvas.drawText(badge, bx, by + 0.5f * density, paint);
        paint.setTextSize(10.5f * density);
      }
    }

    // 8. Floating revenue popup on recent trip completion
    long now = android.os.SystemClock.uptimeMillis();
    if (popupText != null && popupCell >= 0 && now < popupUntilMs) {
      float frac = 1f - (popupUntilMs - now) / 2200f;
      float px = screenX(popupCell);
      float py = screenY(popupCell) - unit * zoom - frac * 26 * density;
      paint.setTextSize(12 * density);
      paint.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
      float pw = paint.measureText(popupText) + 14 * density;
      fill(0xee162b22);
      rect.set(px - pw / 2, py - 13 * density, px + pw / 2, py + 4 * density);
      canvas.drawRoundRect(rect, 8 * density, 8 * density, paint);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(1.2f * density);
      paint.setColor(0xff7ee2b8);
      canvas.drawRoundRect(rect, 8 * density, 8 * density, paint);
      fill(0xff9ef7cc);
      canvas.drawText(popupText, px, py - 1 * density, paint);
    }

    paint.setTextAlign(Paint.Align.LEFT);
    paint.setTextSize(9.5f * density);
    fill(night ? 0xff9fb4cc : 0xff48614d);
    canvas.drawText(
        "TACTICAL CITY BOARD  ·  TAP HUB TO MANAGE  ·  OFFLINE",
        14 * density,
        getHeight() - 12 * density,
        paint);
  }

  private void vehicle(
      Canvas canvas,
      int cell,
      int at,
      int[] path,
      double progress,
      int color,
      float radius,
      int id) {
    float x = cellX(cell), y = cellY(cell), angle = 0;
    if (path != null && path.length == 1) x += unit * 1.35f;
    if (path != null && at >= 0 && at + 1 < path.length) {
      int next = path[at + 1];
      float dx = cellX(next) - x, dy = cellY(next) - y;
      x += dx * (float) progress;
      y += dy * (float) progress;
      angle = (float) Math.toDegrees(Math.atan2(dy, dx));
    }
    canvas.save();
    canvas.translate(x, y);
    canvas.rotate(angle);
    // Drop shadow
    fill(0x66000000);
    rect.set(-radius * 1.45f, -radius * .55f, radius * 1.45f, radius * .80f);
    canvas.drawRoundRect(rect, radius * .35f, radius * .35f, paint);
    // Headlight glow in night mode
    if (night && id > 0) {
      fill(0x38fff2b2);
      canvas.drawCircle(radius * 1.55f, 0, radius * .65f, paint);
    }
    // Vehicle body
    fill(color);
    rect.set(-radius * 1.42f, -radius * .68f, radius * 1.42f, radius * .68f);
    canvas.drawRoundRect(rect, radius * .34f, radius * .34f, paint);
    // Cabin glass
    fill(0xff142538);
    rect.set(-radius * .55f, -radius * .50f, radius * .55f, radius * .50f);
    canvas.drawRoundRect(rect, radius * .18f, radius * .18f, paint);
    // Roof taxi lamp
    if (id > 0) {
      fill(0xfffff4cc);
      rect.set(-radius * .20f, -radius * .22f, radius * .20f, radius * .22f);
      canvas.drawRoundRect(rect, radius * .10f, radius * .10f, paint);
    }
    canvas.restore();
  }
}
