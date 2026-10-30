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
  private final GestureDetector gestures;
  private final ScaleGestureDetector pinch;
  private Game game;
  private Listener listener;
  private float zoom = 1, panX = 0, panY = 0, unit = 1, left = 0, top = 0;
  private boolean night = true;

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
    int ground = night ? 0xff142923 : 0xffdce2cf,
        block = night ? 0xff203b31 : 0xffcad4bf,
        road = night ? 0xff456052 : 0xffa9b6a0;
    canvas.drawColor(ground);
    float density = getResources().getDisplayMetrics().density;
    unit = Math.min(getWidth() / (float) Game.W, getHeight() / (float) Game.H) * .94f;
    left = (getWidth() - Game.W * unit) / 2;
    top = (getHeight() - Game.H * unit) / 2;
    canvas.save();
    canvas.translate(left + panX + Game.W * unit / 2, top + panY + Game.H * unit / 2);
    canvas.scale(zoom, zoom);
    canvas.translate(-Game.W * unit / 2, -Game.H * unit / 2);
    for (int y = 0; y < Game.H; y++)
      for (int x = 0; x < Game.W; x++) {
        if (Game.road(x, y)) continue;
        int color =
            Game.water(x, y)
                ? (night ? 0xff234d4b : 0xff9abdbc)
                : Game.park(x, y) ? (night ? 0xff2e5540 : 0xffb0c7a0) : block;
        fill(color);
        rect.set(
            x * unit + unit * .10f,
            y * unit + unit * .10f,
            (x + 1) * unit - unit * .10f,
            (y + 1) * unit - unit * .10f);
        canvas.drawRoundRect(rect, unit * .15f, unit * .15f, paint);
        if (!Game.water(x, y) && !Game.park(x, y) && (x + y) % 3 == 0) {
          fill(night ? 0xff294638 : 0xffbdcbb4);
          canvas.drawRect(
              x * unit + .3f * unit,
              y * unit + .2f * unit,
              (x + .65f) * unit,
              (y + .75f) * unit,
              paint);
        }
      }
    paint.setStrokeCap(Paint.Cap.ROUND);
    for (int x = 0; x < Game.W; x += 5) {
      paint.setColor(road);
      paint.setStrokeWidth(x == 15 ? unit * .55f : unit * .30f);
      canvas.drawLine((x + .5f) * unit, unit * .5f, (x + .5f) * unit, (Game.H - .5f) * unit, paint);
    }
    for (int y = 0; y < Game.H; y += 4) {
      paint.setColor(road);
      paint.setStrokeWidth(y == 4 || y == 16 ? unit * .55f : unit * .30f);
      canvas.drawLine(unit * .5f, (y + .5f) * unit, (Game.W - .5f) * unit, (y + .5f) * unit, paint);
    }
    for (int cell = 0; cell < game.corridorTier.length; cell++)
      if (game.corridorTier[cell] > 0) {
        int tier = game.corridorTier[cell];
        paint.setColor(tier == 3 ? 0xffedbb71 : tier == 2 ? 0xffa7d7c4 : 0xff648f75);
        paint.setStrokeWidth(unit * (tier == 1 ? .14f : .23f));
        if (cell % Game.W < Game.W - 1 && game.corridorTier[cell + 1] > 0)
          canvas.drawLine(cellX(cell), cellY(cell), cellX(cell + 1), cellY(cell + 1), paint);
        if (cell / Game.W < Game.H - 1 && game.corridorTier[cell + Game.W] > 0)
          canvas.drawLine(
              cellX(cell), cellY(cell), cellX(cell + Game.W), cellY(cell + Game.W), paint);
      }
    for (Game.Company company : game.companies)
      if (!company.merged)
        vehicle(
            canvas,
            company.cell,
            company.at,
            company.path,
            company.progress,
            company.color,
            unit * .27f);
    for (Game.Taxi taxi : game.taxis)
      vehicle(
          canvas,
          taxi.cell,
          taxi.at,
          taxi.path,
          taxi.progress,
          taxi.broken ? 0xffed927d : taxi.status == Game.RIDING ? 0xffc6f0d0 : 0xfff3bd70,
          unit * .45f);
    for (int i = 0; i < game.districts.size(); i++) {
      Game.District hub = game.districts.get(i);
      float x = cellX(hub.cell()), y = cellY(hub.cell());
      fill(night ? 0xff10271f : 0xffeff1df);
      canvas.drawCircle(x, y, unit * .92f, paint);
      paint.setColor(hub.open ? hub.color : 0xff74897e);
      paint.setStyle(Paint.Style.STROKE);
      paint.setStrokeWidth(unit * .17f);
      canvas.drawCircle(x, y, unit * .74f, paint);
      fill(hub.open ? 0xfff3edce : 0xff91a79b);
      paint.setTextSize(unit * .7f);
      paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
      paint.setTextAlign(Paint.Align.CENTER);
      canvas.drawText(hub.open ? String.valueOf(hub.level) : "+", x, y + unit * .25f, paint);
    }
    canvas.restore();
    // Screen-space labels remain readable when the camera zooms.
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    paint.setTextSize(11 * density);
    for (Game.District hub : game.districts) {
      String name = hub.name;
      float x = screenX(hub.cell()), y = screenY(hub.cell()) + unit * zoom + 14 * density;
      float width = paint.measureText(name) + 14 * density;
      x = Math.max(width / 2 + 3, Math.min(getWidth() - width / 2 - 3, x));
      fill(night ? 0xdd12271f : 0xeef0efdf);
      rect.set(x - width / 2, y - 12 * density, x + width / 2, y + 5 * density);
      canvas.drawRoundRect(rect, 5 * density, 5 * density, paint);
      fill(night ? 0xffe3ead7 : 0xff263e30);
      canvas.drawText(name, x, y, paint);
    }
    paint.setTextAlign(Paint.Align.LEFT);
    paint.setTextSize(10 * density);
    fill(night ? 0xffb1c1ae : 0xff48614d);
    canvas.drawText(
        "ORIGINAL CITY SCHEMATIC  ·  OFFLINE", 14 * density, getHeight() - 12 * density, paint);
  }

  private void vehicle(
      Canvas canvas, int cell, int at, int[] path, double progress, int color, float radius) {
    float x = cellX(cell), y = cellY(cell), angle = 0;
    if (path != null && path.length == 1) x += unit * 1.4f;
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
    fill(color);
    rect.set(-radius * 1.4f, -radius * .65f, radius * 1.4f, radius * .65f);
    canvas.drawRoundRect(rect, radius * .3f, radius * .3f, paint);
    fill(0xff19352e);
    canvas.drawRect(-radius * .45f, -radius * .46f, radius * .45f, radius * .46f, paint);
    canvas.restore();
  }
}
