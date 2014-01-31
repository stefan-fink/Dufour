package ch.trillian.dufour;

public final class Ch1903 {

  public static final void wgs84toCh1903(double latitude, double longitude, double altitude, double[] result) {

    // calculate ch1903 coordinates
    double p = (latitude * 3600.0 - 169028.66) / 10000.0;
    double l = (longitude * 3600.0 - 26782.5) / 10000.0;
    double x = 200147.07 + 308807.95 * p + 3745.25 * l * l + 76.63 * p * p + 119.79 * p * p * p - 194.56 * l * l * p;
    double y = 600072.37 + 211455.93 * l - 10938.51 * l * p - 0.36 * l * p * p - 44.54 * l * l * l;
    double h = altitude - 49.55 + 2.73 * l + 6.94 * p;

    result[0] = x;
    result[1] = y;
    result[2] = h;
  }

  public static final void ch1903toWgs84to(double x, double y, double h, double[] result) {

    // calculate wgs84 coordinates
    y = (y - 600000.0) / 1000000.0;
    x = (x - 200000.0) / 1000000.0;

    double l = 2.6779094 + 4.728982 * y + 0.791484 * y * x + 0.1306 * y * x * x - 0.0436 * y * y * y;
    double p = 16.9023892 + 3.238272 * x - 0.270978 * y * y - 0.002528 * x * x - 0.0447 * y * y * x - 0.0140 * x * x * x;
    double a = h + 49.55 - 12.60 * y - 22.64 * x;
    l = l * 100 / 36;
    p = p * 100 / 36;

    result[0] = p;
    result[1] = l;
    result[2] = a;
  }
}
