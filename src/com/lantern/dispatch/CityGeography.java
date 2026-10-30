package com.lantern.dispatch;

/** Geographic anchors for game hubs. Service corridors remain a simulation, not road navigation. */
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

  static double[] hub(int city, int index) {
    return HUBS[city][index].clone();
  }

  static double[] position(int city, int cell, int next, double progress) {
    double x = cell % Game.W + (next % Game.W - cell % Game.W) * progress;
    double y = cell / Game.W + (next / Game.W - cell / Game.W) * progress;
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
}
