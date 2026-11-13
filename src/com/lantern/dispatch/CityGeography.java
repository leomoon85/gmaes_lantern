package com.lantern.dispatch;

/** Geographic anchors for game hubs & street/bridge corridors. */
final class CityGeography {
  static final double[][][] HUBS = {
    {
      {12.9601, 77.5747},
      {12.9784, 77.6408},
      {13.1986, 77.7066},
      {12.9767, 77.5713},
      {12.9698, 77.7500},
      {12.9352, 77.6245}
    },
    {
      {51.4995, -.1248},
      {51.5054, -.0235},
      {51.4700, -.4543},
      {51.5308, -.1238},
      {51.5055, -.0909},
      {51.4826, -.0077}
    },
    {
      {40.7580, -73.9855},
      {40.7075, -74.0113},
      {40.6413, -73.7781},
      {40.7282, -74.0776},
      {40.6782, -73.9442},
      {40.8448, -73.8648}
    },
    {
      {35.6938, 139.7034},
      {35.6984, 139.7730},
      {35.5494, 139.7798},
      {35.6285, 139.7388},
      {35.6580, 139.7016},
      {35.6267, 139.7753}
    },
    {
      {37.7749, -122.4194},
      {37.7786, -122.4055},
      {37.6213, -122.3790},
      {37.7599, -122.4148},
      {37.8080, -122.4177},
      {37.7290, -122.3920}
    },
    {
      {1.2838, 103.8591},
      {1.2997, 103.7879},
      {1.3644, 103.9915},
      {1.2640, 103.6179},
      {1.3048, 103.8318},
      {1.2494, 103.8303}
    }
  };

  /**
   * Street-and-bridge polylines for London corridors so vehicles follow dry-land arterial roads and
   * real Thames bridges instead of cutting across the river. Each entry maps {gridCoord, lat, lon}.
   */
  // y=4: Westminster(5) -> Canary Wharf(15) via Victoria Embankment & Commercial Rd (North Bank)
  private static final double[][] LON_TOP_WEST_TO_MID = {
    {5, 51.4995, -.1248},
    {7, 51.5065, -.1210},
    {9, 51.5112, -.1145},
    {11, 51.5134, -.0890},
    {13, 51.5122, -.0560},
    {15, 51.5054, -.0235}
  };
  // y=4: Canary Wharf(15) -> Heathrow(25) via Aldgate, Holborn, Notting Hill & A4 M4 (North Bank)
  private static final double[][] LON_TOP_MID_TO_EAST = {
    {15, 51.5054, -.0235},
    {17, 51.5146, -.0755},
    {19, 51.5174, -.1200},
    {21, 51.5074, -.1970},
    {23, 51.4910, -.2900},
    {25, 51.4700, -.4543}
  };
  // y=16: King's Cross(5) -> Southwark(15) via Blackfriars Bridge (perpendicular Thames crossing)
  private static final double[][] LON_BOT_WEST_TO_MID = {
    {5, 51.5308, -.1238},
    {8, 51.5220, -.1120},
    {10, 51.5122, -.1045},
    {11, 51.5070, -.1045},
    {13, 51.5058, -.0980},
    {15, 51.5055, -.0909}
  };
  // y=16: Southwark(15) -> Greenwich(25) via Tooley St, Jamaica Rd & Creek Rd (South Bank)
  private static final double[][] LON_BOT_MID_TO_EAST = {
    {15, 51.5055, -.0909},
    {17, 51.4995, -.0720},
    {20, 51.4970, -.0490},
    {23, 51.4838, -.0250},
    {25, 51.4826, -.0077}
  };
  // x=5: Westminster(4) -> King's Cross(16) via Charing Cross & Euston Rd (North Bank)
  private static final double[][] LON_COL_LEFT = {
    {4, 51.4995, -.1248},
    {8, 51.5105, -.1280},
    {12, 51.5230, -.1295},
    {16, 51.5308, -.1238}
  };
  // x=15: Canary Wharf(4) -> Southwark(16) via The Highway & Tower Bridge
  private static final double[][] LON_COL_MID = {
    {4, 51.5054, -.0235},
    {8, 51.5095, -.0550},
    {10, 51.5088, -.0754},
    {12, 51.5022, -.0754},
    {16, 51.5055, -.0909}
  };
  // x=25: Heathrow(4) -> Greenwich(16) via Kew Bridge & South Circular (South Bank)
  private static final double[][] LON_COL_RIGHT = {
    {4, 51.4700, -.4543},
    {7, 51.4885, -.2875},
    {8, 51.4820, -.2865},
    {12, 51.4640, -.1650},
    {14, 51.4755, -.0450},
    {16, 51.4826, -.0077}
  };

  static double[] hub(int city, int index) {
    return HUBS[city][index].clone();
  }

  private static double[] samplePolyline(double[][] pts, double coord) {
    if (coord <= pts[0][0]) return new double[] {pts[0][1], pts[0][2]};
    for (int i = 1; i < pts.length; i++) {
      if (coord <= pts[i][0]) {
        double span = Math.max(1e-6, pts[i][0] - pts[i - 1][0]);
        double t = (coord - pts[i - 1][0]) / span;
        return new double[] {
          pts[i - 1][1] + (pts[i][1] - pts[i - 1][1]) * t,
          pts[i - 1][2] + (pts[i][2] - pts[i - 1][2]) * t
        };
      }
    }
    double[] last = pts[pts.length - 1];
    return new double[] {last[1], last[2]};
  }

  private static double[] londonCellLatLng(double x, double y) {
    // Exact hub matches
    if (Math.abs(y - 4) < 0.01) {
      return x <= 15
          ? samplePolyline(LON_TOP_WEST_TO_MID, x)
          : samplePolyline(LON_TOP_MID_TO_EAST, x);
    }
    if (Math.abs(y - 16) < 0.01) {
      return x <= 15
          ? samplePolyline(LON_BOT_WEST_TO_MID, x)
          : samplePolyline(LON_BOT_MID_TO_EAST, x);
    }
    if (Math.abs(x - 5) < 0.01) return samplePolyline(LON_COL_LEFT, y);
    if (Math.abs(x - 15) < 0.01) return samplePolyline(LON_COL_MID, y);
    if (Math.abs(x - 25) < 0.01) return samplePolyline(LON_COL_RIGHT, y);

    // Interior grid streets blend smoothly between the top/bottom/column arterial corridors
    double v = Math.max(0, Math.min(1, (y - 4) / 12.0));
    double[] top =
        x <= 15 ? samplePolyline(LON_TOP_WEST_TO_MID, x) : samplePolyline(LON_TOP_MID_TO_EAST, x);
    double[] bot =
        x <= 15 ? samplePolyline(LON_BOT_WEST_TO_MID, x) : samplePolyline(LON_BOT_MID_TO_EAST, x);
    return new double[] {
      top[0] * (1 - v) + bot[0] * v,
      top[1] * (1 - v) + bot[1] * v
    };
  }

  private static double[] cellLatLng(int city, double x, double y) {
    if (city == 1) return londonCellLatLng(x, y);
    double u = Math.max(0, Math.min(2, (x - 5) / 10)), v = Math.max(0, Math.min(1, (y - 4) / 12));
    int col = Math.min(1, (int) u);
    double f = u - col;
    double[] result = new double[2];
    for (int k = 0; k < 2; k++) {
      double top = HUBS[city][col][k] * (1 - f) + HUBS[city][col + 1][k] * f;
      double bottom = HUBS[city][col + 3][k] * (1 - f) + HUBS[city][col + 4][k] * f;
      result[k] = top * (1 - v) + bottom * v;
    }
    return result;
  }

  static double[] position(int city, int cell, int next, double progress) {
    double[] a = cellLatLng(city, cell % Game.W, cell / Game.W);
    if (next == cell || progress <= 0) return a;
    double[] b = cellLatLng(city, next % Game.W, next / Game.W);
    return new double[] {
      a[0] + (b[0] - a[0]) * progress,
      a[1] + (b[1] - a[1]) * progress
    };
  }
}
