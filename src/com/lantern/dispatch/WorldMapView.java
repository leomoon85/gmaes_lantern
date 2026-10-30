package com.lantern.dispatch;

import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;
import java.util.*;
import org.maplibre.android.MapLibre;
import org.maplibre.android.annotations.*;
import org.maplibre.android.camera.CameraUpdateFactory;
import org.maplibre.android.geometry.*;
import org.maplibre.android.maps.*;

/** Native street map backed by MapLibre/OpenFreeMap, with an always-playable offline city. */
final class WorldMapView extends FrameLayout {
  private static final String DARK = "https://tiles.openfreemap.org/styles/dark";
  private static final String DAY = "https://tiles.openfreemap.org/styles/liberty";
  private final CityMapView fallback;
  private final Handler main = new Handler(Looper.getMainLooper());
  private MapView mapView;
  private MapLibreMap streetMap;
  private Game game;
  private CityMapView.Listener listener;
  private boolean night = true, streets = true, loaded = false, destroyed = false;
  private int city = -1, networkHash = 0;
  private long lastUpdate;
  private String status = "Connecting street map…";
  private final Map<Integer, Marker> hubs = new HashMap<>(), vehicles = new HashMap<>();
  private final Map<Integer, Marker> rivalVehicles = new HashMap<>();
  private final ArrayList<Polyline> corridors = new ArrayList<>();
  private final HashMap<Integer, Icon> markerIcons = new HashMap<>();
  private final Runnable timeout = this::handleTimeout;

  private void handleTimeout() {
    if (!loaded && !destroyed) {
      fallback.setVisibility(VISIBLE);
      status = "Street map unavailable · offline city ready";
    }
  }

  WorldMapView(Context context) {
    this(context, null);
  }

  WorldMapView(Context context, Bundle state) {
    super(context);
    setBackgroundColor(0xff0c1422);
    fallback = new CityMapView(context);
    addView(fallback, new LayoutParams(-1, -1));
    try {
      MapLibre.getInstance(context.getApplicationContext());
      mapView = new MapView(context);
      addView(mapView, 0, new LayoutParams(-1, -1));
      mapView.onCreate(state);
      mapView.addOnDidFinishLoadingMapListener(
          () -> {
            if (destroyed || !streets) return;
            loaded = true;
            fallback.setVisibility(GONE);
            status = "OpenFreeMap · simulated service corridors";
            main.removeCallbacks(timeout);
          });
      mapView.addOnDidFailLoadingMapListener(
          error -> {
            if (!destroyed) {
              loaded = false;
              fallback.setVisibility(VISIBLE);
              status = "Offline city · street map unavailable";
            }
          });
      mapView.getMapAsync(
          map -> {
            if (destroyed) return;
            streetMap = map;
            map.getUiSettings().setCompassEnabled(true);
            map.getUiSettings().setAttributionEnabled(true);
            map.getUiSettings().setLogoEnabled(true);
            map.setOnMarkerClickListener(
                marker -> {
                  for (Map.Entry<Integer, Marker> entry : hubs.entrySet())
                    if (entry.getValue().equals(marker)) {
                      if (listener != null) listener.hubSelected(entry.getKey());
                      return true;
                    }
                  return false;
                });
            loadStyle();
          });
      main.postDelayed(timeout, 12000);
    } catch (RuntimeException | LinkageError error) {
      status = "Street map unavailable · offline city ready";
      if (mapView != null) {
        removeView(mapView);
        mapView = null;
      }
    }
  }

  void bind(Game value, CityMapView.Listener callback) {
    game = value;
    listener = callback;
    fallback.bind(value, callback);
    city = -1;
    networkHash = 0;
    tick();
  }

  void setNight(boolean value) {
    boolean changed = night != value;
    night = value;
    fallback.setNight(value);
    if (changed && streetMap != null) loadStyle();
  }

  String status() {
    return status;
  }

  boolean isUsingStreets() {
    return streets;
  }

  void toggleStreets() {
    if (streets && !loaded && streetMap != null) {
      loadStyle();
      return;
    }
    streets = !streets;
    if (!streets) {
      fallback.setVisibility(VISIBLE);
      if (mapView != null) mapView.setVisibility(GONE);
      status = "Offline city · ready to play";
    } else {
      if (mapView != null) mapView.setVisibility(VISIBLE);
      if (loaded) {
        fallback.setVisibility(GONE);
        status = "OpenFreeMap · simulated service corridors";
      } else loadStyle();
    }
  }

  void resetCamera() {
    fallback.resetCamera();
    if (streetMap != null && game != null) {
      ArrayList<LatLng> points = new ArrayList<>();
      for (int i = 0; i < 6; i++) points.add(hub(i));
      if (getWidth() > 0 && getHeight() > 0) {
        float density = getResources().getDisplayMetrics().density;
        streetMap.setPadding(
            Math.round(12 * density),
            Math.min(Math.round(130 * density), getHeight() / 4),
            Math.round(12 * density),
            Math.min(Math.round(170 * density), getHeight() / 3));
        streetMap.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                new LatLngBounds.Builder().includes(points).build(), Math.round(24 * density)));
      }
    }
  }

  private void loadStyle() {
    if (streetMap == null) {
      status = "Offline city · street renderer unavailable";
      return;
    }
    loaded = false;
    fallback.setVisibility(VISIBLE);
    status = "Connecting street map…";
    main.removeCallbacks(timeout);
    main.postDelayed(timeout, 12000);
    streetMap.setStyle(
        night ? DARK : DAY,
        style -> {
          if (destroyed) return;
          hubs.clear();
          vehicles.clear();
          rivalVehicles.clear();
          corridors.clear();
          city = -1;
          networkHash = 0;
          tick();
          resetCamera();
        });
  }

  private LatLng hub(int index) {
    double[] point = CityGeography.hub(game.scenarioId, index);
    return new LatLng(point[0], point[1]);
  }

  void tick() {
    fallback.invalidate();
    if (game == null
        || streetMap == null
        || streetMap.getStyle() == null
        || !streetMap.getStyle().isFullyLoaded()) return;
    long now = android.os.SystemClock.uptimeMillis();
    if (city == game.scenarioId && now - lastUpdate < 120) return;
    lastUpdate = now;
    if (city != game.scenarioId) {
      streetMap.clear();
      hubs.clear();
      vehicles.clear();
      rivalVehicles.clear();
      corridors.clear();
      city = game.scenarioId;
      networkHash = 0;
      for (int i = 0; i < 6; i++) {
        Game.District district = game.districts.get(i);
        hubs.put(
            i,
            streetMap.addMarker(
                new MarkerOptions()
                    .position(hub(i))
                    .icon(hubIcon(district))
                    .title(district.name)
                    .snippet(district.open ? "Level " + district.level : "Available to connect")));
      }
      resetCamera();
    }
    for (int i = 0; i < 6; i++) {
      Game.District district = game.districts.get(i);
      hubs.get(i).setSnippet(district.open ? "Level " + district.level : "Available to connect");
      hubs.get(i).setIcon(hubIcon(district));
    }
    Set<Integer> alive = new HashSet<>();
    for (Game.Taxi vehicle : game.taxis) {
      alive.add(vehicle.id);
      int next =
          vehicle.path != null && vehicle.at + 1 < vehicle.path.length
              ? vehicle.path[vehicle.at + 1]
              : vehicle.cell;
      double[] point = CityGeography.position(city, vehicle.cell, next, vehicle.progress);
      LatLng position = new LatLng(point[0], point[1]);
      Marker marker = vehicles.get(vehicle.id);
      if (marker == null) {
        marker =
            streetMap.addMarker(
                new MarkerOptions()
                    .position(position)
                    .icon(carIcon())
                    .title("Vehicle " + vehicle.id)
                    .snippet(vehicle.tierName()));
        vehicles.put(vehicle.id, marker);
      } else marker.setPosition(position);
    }
    for (Iterator<Map.Entry<Integer, Marker>> it = vehicles.entrySet().iterator(); it.hasNext(); ) {
      Map.Entry<Integer, Marker> entry = it.next();
      if (!alive.contains(entry.getKey())) {
        streetMap.removeMarker(entry.getValue());
        it.remove();
      }
    }
    for (int i = 0; i < game.companies.size(); i++) {
      Game.Company rival = game.companies.get(i);
      Marker marker = rivalVehicles.get(i);
      if (rival.merged) {
        if (marker != null) {
          streetMap.removeMarker(marker);
          rivalVehicles.remove(i);
        }
        continue;
      }
      int next =
          rival.path != null && rival.at + 1 < rival.path.length
              ? rival.path[rival.at + 1]
              : rival.cell;
      double[] point = CityGeography.position(city, rival.cell, next, rival.progress);
      LatLng position = new LatLng(point[0], point[1]);
      if (marker == null)
        rivalVehicles.put(
            i,
            streetMap.addMarker(
                new MarkerOptions()
                    .position(position)
                    .icon(carIcon(rival.color, 101 + i))
                    .title(rival.name)
                    .snippet("Rival operator")));
      else marker.setPosition(position);
    }
    int hash = Arrays.hashCode(game.corridorTier);
    if (hash != networkHash) {
      networkHash = hash;
      for (Polyline line : corridors) streetMap.removePolyline(line);
      corridors.clear();
      for (int i = 1; i < 6; i++)
        if (game.districts.get(i).open) {
          int[] path = game.route(game.districts.get(0).cell(), game.districts.get(i).cell());
          ArrayList<LatLng> points = new ArrayList<>();
          for (int cell : path) {
            double[] p = CityGeography.position(city, cell, cell, 0);
            points.add(new LatLng(p[0], p[1]));
          }
          corridors.add(
              streetMap.addPolyline(
                  new PolylineOptions().addAll(points).color(Color.rgb(250, 189, 98)).width(3)));
        }
    }
  }

  void start() {
    if (mapView != null) mapView.onStart();
  }

  private Icon hubIcon(Game.District district) {
    int key = district.open ? district.level : 0;
    Icon cached = markerIcons.get(key);
    if (cached != null) return cached;
    float d = getResources().getDisplayMetrics().density;
    Bitmap image =
        Bitmap.createBitmap(Math.round(44 * d), Math.round(44 * d), Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(image);
    c.scale(d, d);
    Paint paint = new Paint(3);
    paint.setColor(0x443d658d);
    c.drawCircle(22, 22, 20, paint);
    paint.setColor(0xff101d30);
    c.drawCircle(22, 22, 14, paint);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(2.5f);
    paint.setColor(district.open ? 0xfff3bd70 : 0xff778ba5);
    c.drawCircle(22, 22, 14, paint);
    paint.setStyle(Paint.Style.FILL);
    paint.setTextAlign(Paint.Align.CENTER);
    paint.setTextSize(14);
    paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    c.drawText(district.open ? String.valueOf(district.level) : "+", 22, 27, paint);
    Icon icon = IconFactory.getInstance(getContext()).fromBitmap(image);
    markerIcons.put(key, icon);
    return icon;
  }

  private Icon carIcon() {
    return carIcon(0xfff3bd70, 100);
  }

  private Icon carIcon(int color, int key) {
    Icon cached = markerIcons.get(key);
    if (cached != null) return cached;
    float d = getResources().getDisplayMetrics().density;
    Bitmap image =
        Bitmap.createBitmap(Math.round(36 * d), Math.round(24 * d), Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(image);
    c.scale(d, d);
    Paint paint = new Paint(3);
    paint.setColor(color);
    c.drawRoundRect(3, 5, 33, 20, 4, 4, paint);
    paint.setColor(0xff142238);
    c.drawRoundRect(12, 7, 23, 18, 2, 2, paint);
    paint.setColor(0xfffff3ce);
    c.drawRect(28, 7, 31, 10, paint);
    c.drawRect(28, 15, 31, 18, paint);
    Icon icon = IconFactory.getInstance(getContext()).fromBitmap(image);
    markerIcons.put(key, icon);
    return icon;
  }

  void resume() {
    if (mapView != null) mapView.onResume();
  }

  void pause() {
    if (mapView != null) mapView.onPause();
  }

  void stop() {
    if (mapView != null) mapView.onStop();
  }

  void destroy() {
    destroyed = true;
    main.removeCallbacksAndMessages(null);
    if (mapView != null) mapView.onDestroy();
  }

  void lowMemory() {
    if (mapView != null) mapView.onLowMemory();
  }

  void saveState(Bundle state) {
    if (mapView != null) mapView.onSaveInstanceState(state);
  }
}
