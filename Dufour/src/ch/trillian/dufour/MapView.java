package ch.trillian.dufour;

import java.text.SimpleDateFormat;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MapView extends View {

  private static final int INVALID_POINTER_ID = -1;

  // the map 
  private Map map;
  private Layer layer;
  private int layerIndex;
  
  // attributes
  private int textSize;
  private int gridStroke;
  private int gridTextSize;
  private int crossSize;
  private int crossStroke;

  private String labelText = "This is a label";
  private Bitmap bitmap = null;

  // painters and paths
  private Paint textPaint;
  private Paint gridLinePaint;
  private Paint gridTextPaint;
  private Paint crossPaint;

  // view size in pixel
  private int sizeX;
  private int sizeY;

  // position and zoom
  // pixelX = (positionX + x) * scale 
  // x = pixelX / scale - positionX
  private float positionX = 500;
  private float positionY = 500;
  private float scale = 1.0f;

  // min and max coordinates of tiles on screen
  private int minTileX;
  private int maxTileX;
  private int minTileY;
  private int maxTileY;

  // stuff for motion detection
  float lastTouchX;
  float lastTouchY;
  int activePointerId;

  // gesture detectors
  ScaleGestureDetector mScaleGestureDetector;
  GestureDetector mGestureDetector;

  // current location stuff
  private Location location;
  private String latitudeText;
  private String longitudeText;
  private String altitudeText;
  private String accuracyText;
  private String speedText;
  private String bearingText;
  private String timeText;

  public MapView(Context context, AttributeSet attrs) {

    super(context, attrs);

    // get attributes
    TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MapView, 0, 0);
    try {
      textSize = a.getDimensionPixelSize(R.styleable.MapView_textSize, 10);
      gridStroke = a.getDimensionPixelSize(R.styleable.MapView_gridStroke, 1);
      gridTextSize = a.getDimensionPixelSize(R.styleable.MapView_gridTextSize, 10);
      crossSize = a.getDimensionPixelSize(R.styleable.MapView_crossSize, 10);
      crossStroke = a.getDimensionPixelSize(R.styleable.MapView_crossStroke, 1);
    } finally {
      a.recycle();
    }

    Log.w("TRILLIAN", "textSize=" + textSize);
    Log.w("TRILLIAN", "crossSize=" + crossSize);
    Log.w("TRILLIAN", "crossStroke=" + crossStroke);

    // init painters
    initPainters();

    // Create our ScaleGestureDetector
    mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    mGestureDetector = new GestureDetector(context, new GestureListener());
  }

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {

      float newScale = scale * detector.getScaleFactor();

      // Don't let the object get too small or too large.
      newScale = Math.max(0.25f, Math.min(newScale, 4.0f));

      float focusX = detector.getFocusX() / scale - positionX;
      float focusY = detector.getFocusY() / scale - positionY;

      positionX = (positionX + focusX) * scale / newScale - focusX;
      positionY = (positionY + focusY) * scale / newScale - focusY;

      scale = newScale;

      invalidate();
      return true;
    }
  }

  private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onDoubleTap(MotionEvent e) {

      // reset viewport
      scale = 1.0f;
      positionX = sizeX / 2;
      positionY = sizeY / 2;

      invalidate();
      return true;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {

    sizeX = w;
    sizeY = h;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {

    // let the ScaleGestureDetector inspect all events.
    mScaleGestureDetector.onTouchEvent(ev);

    // let the GestureDetector inspect all events.
    mGestureDetector.onTouchEvent(ev);

    final int pointerIndex;
    final float x;
    final float y;

    final int action = ev.getAction();
    switch (action & MotionEvent.ACTION_MASK) {

    case MotionEvent.ACTION_DOWN:

      // remember last touch
      lastTouchX = ev.getX();
      lastTouchY = ev.getY();
      activePointerId = ev.getPointerId(0);

      break;

    case MotionEvent.ACTION_MOVE:

      pointerIndex = ev.findPointerIndex(activePointerId);
      x = ev.getX(pointerIndex);
      y = ev.getY(pointerIndex);

      // only move if the ScaleGestureDetector isn't processing a gesture.
      if (!mScaleGestureDetector.isInProgress()) {

        // calculate the distance moved
        final float dx = x - lastTouchX;
        final float dy = y - lastTouchY;

        // move the viewport
        positionX += dx / scale;
        positionY += dy / scale;

        invalidate();
      }

      // Remember this touch position for the next move event
      lastTouchX = x;
      lastTouchY = y;

      break;

    case MotionEvent.ACTION_UP:
    case MotionEvent.ACTION_CANCEL:
      activePointerId = INVALID_POINTER_ID;
      break;

    case MotionEvent.ACTION_POINTER_UP:

      // Extract the index of the pointer that left the touch sensor
      pointerIndex = (action & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
      final int pointerId = ev.getPointerId(pointerIndex);

      // If it was our active pointer going up then choose a new active pointer and adjust accordingly.
      if (pointerId == activePointerId) {
        final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
        lastTouchX = ev.getX(newPointerIndex);
        lastTouchY = ev.getY(newPointerIndex);
        activePointerId = ev.getPointerId(newPointerIndex);
      }
      break;
    }

    return true;
  }

  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);

    Log.w("TRILLIAN", "onDraw: " + System.currentTimeMillis());
    
    if (location != null) {
      textPaint.setTextSize(textSize / 2);
      float y = 0 - textPaint.ascent();
      canvas.drawText(String.format("x: %4.0f, y: %4.0f %% %4.1f", positionX, positionY, 100 * scale), 0, 0 - textPaint.ascent(), textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(longitudeText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(latitudeText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(altitudeText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(speedText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(bearingText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(accuracyText, 0, y, textPaint);
      y += textPaint.getTextSize();
      canvas.drawText(timeText, 0, y, textPaint);
    }

    canvas.save();
    canvas.scale(scale, scale);
    canvas.translate(positionX, positionY);

    if (bitmap != null) {
      canvas.drawBitmap(bitmap, -256, 0, crossPaint);
    }
    
    // scale grid stroke size
    gridLinePaint.setStrokeWidth(gridStroke / scale);

    // draw axes
    canvas.drawLine(-1000, 0, 1000, 0, gridLinePaint);
    canvas.drawLine(0, -1000, 0, 1000, gridLinePaint);

    // draw grid
    updateTilesMinMax();
    
    float incX = layer.getTileSizeX();
    float incY = layer.getTileSizeY();
    float minX = minTileX * incX;
    float maxX = (maxTileX + 1) * incX;
    float minY = minTileY * incY;
    float maxY = (maxTileY + 1) * incY;

    float x = minX;
    for(int i = minTileX; i <= maxTileX + 1; i++) {
      canvas.drawLine(x, minY, x, maxY, gridLinePaint);
      x += incX;
    }
    float y = minY;
    for(int j = minTileY; j <= maxTileY + 1; j++) {
      canvas.drawLine(minX, y, maxX, y, gridLinePaint);
      y += incY;
    }
    
    // draw grid coordinates
    x = minX + incX / 2;
    for(int i = minTileX; i <= maxTileX; i++) {
      y = minY + incY / 2 + gridTextPaint.getTextSize() / 2;
      for(int j = minTileY; j <= maxTileY; j++) {
        canvas.drawText("(" + i + "," + j + ")", x, y, gridTextPaint);
        y += incY;
      }
      x += incX;
    }
    
    
    canvas.restore();

    // draw cross
    canvas.save();
    canvas.translate(sizeX / 2, sizeY / 2);
    canvas.drawCircle(0, 0, crossSize * 0.5f, crossPaint);
    canvas.drawLine(-crossSize, 0, crossSize, 0, crossPaint);
    canvas.drawLine(0, -crossSize, 0, crossSize, crossPaint);
    canvas.restore();
  }

  private void updateTilesMinMax() {
    
    minTileX = (int) Math.floor(-positionX / layer.getTileSizeX());
    maxTileX = (int) Math.floor((sizeX / scale - positionX) / layer.getTileSizeX());
    minTileY = (int) Math.floor(-positionY / layer.getTileSizeY());
    maxTileY = (int) Math.floor((sizeY / scale - positionY) / layer.getTileSizeY());
  }
  
  public String getLabelText() {

    return labelText;
  }

  public void setLabelText(String labelText) {

    this.labelText = labelText;
    invalidate();
  }

  public void setBitmap(Bitmap bitmap) {

    this.bitmap = bitmap;
    invalidate();
  }

  public Location getLocation() {

    return location;
  }

  public void wgs84toCh1903(double latitude, double longitude, double altitude, double[] result) {

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

  public void ch1903toWgs84to(double x, double y, double h, double[] result) {

    // calculate wgs84 coordinates
    y = (y - 600000) / 1000000.0;
    x = (x - 200000) / 1000000.0;

    double l = 2.6779094 + 4.728982 * y + 0.791484 * y * x + 0.1306 * y * x * x - 0.0436 * y * y * y;
    double p = 16.9023892 + 3.238272 * x - 0.270978 * y * y - 0.002528 * x * x - 0.0447 * y * y * x - 0.0140 * x * x * x;
    double a = h + 49.55 - 12.60 * y - 22.64 * x;
    l = l * 100 / 36;
    p = p * 100 / 36;

    result[0] = p;
    result[1] = l;
    result[2] = a;
  }

  public void setLocation(Location location) {

    this.location = location;

    // convert to ch1903
    double[] result = new double[3];
    wgs84toCh1903(location.getLatitude(), location.getLongitude(), location.getAltitude(), result);
    double x = result[0];
    double y = result[1];
    double h = result[2];

    // prepare texts
    latitudeText = String.format("Latitidue: %3.0f", x);
    longitudeText = String.format("Longitude: %3.0f", y);
    altitudeText = location.hasAltitude() ? String.format("Altitude: %3.0f m", h) : "Altitude: -";
    speedText = location.hasSpeed() ? String.format("Speed: %3.0f km/h", location.getSpeed() * 3.6) : "Speed: -";
    bearingText = location.hasBearing() ? String.format("Bearing: %3.0f", location.getBearing()) : "Bearing: -";
    accuracyText = location.hasAccuracy() ? String.format("Accuracy: %3.0f m", location.getAccuracy()) : "Accuracy: -";
    timeText = String.format("Time: %s", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(location.getTime()));

    invalidate();
  }

  private void initPainters() {

    textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    textPaint.setColor(0xFF000000);
    textPaint.setStrokeWidth(1);
    textPaint.setTextAlign(Paint.Align.LEFT);

    gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    gridLinePaint.setColor(0xFF000000);
    gridLinePaint.setStyle(Paint.Style.STROKE);
    gridLinePaint.setStrokeWidth(crossStroke);

    gridTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    gridTextPaint.setColor(0xFF808080);
    gridTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    gridTextPaint.setTextSize(gridTextSize);
    gridTextPaint.setTextAlign(Paint.Align.CENTER);

    crossPaint = new Paint(0);
    crossPaint.setStyle(Paint.Style.STROKE);
    crossPaint.setStrokeWidth(crossStroke);
  }
  
  public void setMap(Map map, int layerIndex) {
    
    this.map = map;
    this.layerIndex = layerIndex;
    this.layer = map.getLayers()[layerIndex];
    
    invalidate();
  }
}
