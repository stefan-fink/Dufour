package ch.trillian.dufour;

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

  public interface ViewListener {
    
    public void onSizeChanged(int w, int h, int oldw, int oldh);

    public Tile onGetTile(Layer layer, int x, int y);
  }
  
  // view listener (our activity)
  private ViewListener viewListener;
  
  private static final int INVALID_POINTER_ID = -1;

  // the map 
  private Layer layer;
  
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
  private Paint tilePaint;

  // screen size in pixel
  private int screenSizeX;
  private int screenSizeY;

  // center position in pixel
  private float centerX;
  private float centerY;
  
  // position and zoom
  // pixelX = (positionX + x) * scale 
  // x = pixelX / scale - positionX
  private float positionX = 500;
  private float positionY = 500;
  private float scale = 2.0f;

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
  private Location lastGpsLocation;
  private boolean trackGps;

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

  public void scale(float scaleFactor) {
    
    scale(scaleFactor, (float) screenSizeX / 2, (float) screenSizeY / 2);
  }
  
  public void scale(float scaleFactor, float focusScreenX, float focusScreenY) {
    
    float newScale = scale * scaleFactor;

    // focal point in map-pixels
    float focusMapX = screen2map(focusScreenX, scale, positionX);
    float focusMapY = screen2map(focusScreenY, scale, positionY);

    if (newScale > layer.getMaxScale()) {
      
      // try to zoom layer in
      Layer newLayer = layer.getLayerIn();
      if (newLayer != null) {
        float scaleRatio = newLayer.getMeterPerPixel() / layer.getMeterPerPixel();
        newScale *= scaleRatio;
        focusMapX /= scaleRatio;
        focusMapY /= scaleRatio;
        layer = newLayer;
      }
      
    } else if (newScale < layer.getMinScale()) {
      
      // try to zoom layer out
      Layer newLayer = layer.getLayerOut();
      if (newLayer != null) {
        float scaleRatio = newLayer.getMeterPerPixel() / layer.getMeterPerPixel();
        newScale = newScale * scaleRatio;
        focusMapX /= scaleRatio;
        focusMapY /= scaleRatio;
        layer = newLayer;
      }
    }
    
    // Don't let the object get too small or too large.
    newScale = Math.max(layer.getMinScale(), Math.min(newScale, layer.getMaxScale()));

    positionX = focusScreenX / newScale - focusMapX;
    positionY = focusScreenY / newScale - focusMapY;

    scale = newScale;

    invalidate();
  }
  

  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {

      scale(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
      return true;
    }
  }

  public void setViewListener(ViewListener viewListener) {
  
    this.viewListener = viewListener;
  }
  
  private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onDoubleTap(MotionEvent e) {

      // reset viewport
      if (lastGpsLocation != null) {
        trackGps = true;
        setGpsLocation(lastGpsLocation);
      }
      
      return true;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {

    screenSizeX = w;
    screenSizeY = h;
    
    centerX = w / 2f;
    centerY = h / 2f;
    
    if (viewListener != null) {
      viewListener.onSizeChanged(w, h, oldw, oldh);
    }
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

        // disable gps tracking if we moved too far away from GPS location
        if (lastGpsLocation != null && trackGps) {
          
          // calculate screen pixel coordinates of GPS location
          float[] mapPixel = new float[2];
          layer.locationToMapPixel(lastGpsLocation, mapPixel);
          float deltaX = map2screen(mapPixel[0], scale, positionX) - centerX;
          float deltaY = map2screen(mapPixel[1], scale, positionY) - centerY;
          float deltaSquare = deltaX * deltaX + deltaY * deltaY;
          float gpsTrackDistance = Math.min(screenSizeX, screenSizeX) / 10;
          if (deltaSquare > gpsTrackDistance * gpsTrackDistance) {
            trackGps = false;
          }
        }
        
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

  private final float map2screen(float map, float scale, float position) {
    
    return (position + map) * scale;
  }
  
  private final float screen2map(float screen, float scale, float position) {
    
    return screen / scale - position;
  }
  
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);
    
    canvas.save();
    canvas.scale(scale, scale);
    canvas.translate(positionX, positionY);

    if (bitmap != null) {
      canvas.drawBitmap(bitmap, -256, 0, crossPaint);
    }
    
    // scale grid stroke size
    gridLinePaint.setStrokeWidth(gridStroke / scale);

    // draw bitmaps and grids
    updateTilesMinMax();
    
    float incX = layer.getTileSizeX();
    float incY = layer.getTileSizeY();
    float minX = minTileX * incX;
    float maxX = (maxTileX + 1) * incX;
    float minY = minTileY * incY;
    float maxY = (maxTileY + 1) * incY;
    float x, y;
    
    // draw grid lines
    x = minX;
    for(int i = minTileX; i <= maxTileX + 1; i++) {
      canvas.drawLine(x, minY, x, maxY, gridLinePaint);
      x += incX;
    }
    y = minY;
    for(int j = minTileY; j <= maxTileY + 1; j++) {
      canvas.drawLine(minX, y, maxX, y, gridLinePaint);
      y += incY;
    }
    
    // draw bitmaps
    x = minX;
    for(int i = minTileX; i <= maxTileX; i++) {
      y = minY;
      for(int j = minTileY; j <= maxTileY; j++) {
        if (viewListener != null) {
          Tile tile = viewListener.onGetTile(layer, i, j);
          if (tile != null) {
            Bitmap bitmap = tile.getBitmap();
            if (bitmap != null) {
              canvas.drawBitmap(bitmap, x, y, tilePaint);
            }
          }
        }
        y += incY;
      }
      x += incX;
    }
    
    // TODO: order preload tiles here
    
    // draw grid coordinates
    x = minX + incX / 2;
    for(int i = minTileX; i <= maxTileX; i++) {
      y = minY + incY / 2 + gridTextPaint.getTextSize() / 2;
      for(int j = minTileY; j <= maxTileY; j++) {
        String text = layer.hasTile(i, j) ? "(" + i + "," + j + ")" : "no data";
        canvas.drawText(text, x, y, gridTextPaint);
        y += incY;
      }
      x += incX;
    }
    
    // draw gps position
    if (lastGpsLocation != null) {
      canvas.save();
      float[] mapPixel = new float[2]; 
      layer.locationToMapPixel(lastGpsLocation, mapPixel);
      canvas.translate(mapPixel[0], mapPixel[1]);
      canvas.scale(1f/scale, 1f/scale);
      crossPaint.setColor(trackGps ? 0xFFFF0000 : 0xFF0000FF);
      crossPaint.setStyle(Paint.Style.FILL);
      canvas.drawCircle(0, 0, crossSize * 0.25f, crossPaint);
      crossPaint.setStyle(Paint.Style.STROKE);
      crossPaint.setColor(0xff000000);
      canvas.drawCircle(0, 0, crossSize * 0.25f, crossPaint);
      canvas.restore();
    }
    
    canvas.restore();

    // draw coordinates
    String[] displayCoordinates = layer.getDisplayCoordinates(screen2map(centerX, scale, positionX), screen2map(centerY, scale, positionY));
    textPaint.setTextSize(textSize);
    y = 0 - textPaint.ascent();
    canvas.drawText(String.format("%s @ %1.2f [%s, %s]", layer.getName(), scale, displayCoordinates[0], displayCoordinates[1]), 10, y, textPaint);
    textPaint.setColor(0x60808080);
    canvas.drawRect(0f,  0f, screenSizeX, textPaint.getTextSize() + textPaint.descent(), textPaint);
    textPaint.setColor(0xFF000000);
   
    // draw cross
    canvas.save();
    canvas.translate(centerX, centerY);
    canvas.drawCircle(0, 0, crossSize * 0.5f, crossPaint);
    canvas.drawLine(-crossSize, 0, crossSize, 0, crossPaint);
    canvas.drawLine(0, -crossSize, 0, crossSize, crossPaint);
    canvas.restore();
  }

  private void updateTilesMinMax() {
    
    minTileX = (int) Math.floor(-positionX / layer.getTileSizeX());
    maxTileX = (int) Math.floor((screenSizeX / scale - positionX) / layer.getTileSizeX());
    minTileY = (int) Math.floor(-positionY / layer.getTileSizeY());
    maxTileY = (int) Math.floor((screenSizeY / scale - positionY) / layer.getTileSizeY());
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

  public void setGpsLocation(Location location) {

    Log.w("TRILLIAN", "setGpsLocation() trackGps=" + trackGps);
    
    if (location == null) {
      lastGpsLocation = null;
      trackGps = false;
      invalidate();
      return;
    }

    if (lastGpsLocation == null) {
      trackGps = true;
    }
    
    lastGpsLocation = location;

    // center map to gps position if we're tracking
    if (trackGps) {
      float[] mapPixel = new float[2];
      layer.locationToMapPixel(location, mapPixel);
      positionX = centerX / scale - mapPixel[0];
      positionY = centerY / scale - mapPixel[1];
    }
    
//    // convert to ch1903
//    double[] result = new double[3];
//    wgs84toCh1903(location.getLatitude(), location.getLongitude(), location.getAltitude(), result);
//    double x = result[0];
//    double y = result[1];
//    double h = result[2];
//
//    // prepare texts
//    latitudeText = String.format("Latitude: %3.0f", x);
//    longitudeText = String.format("Longitude: %3.0f", y);
//    altitudeText = location.hasAltitude() ? String.format("Altitude: %3.0f m", h) : "Altitude: -";
//    speedText = location.hasSpeed() ? String.format("Speed: %3.0f km/h", location.getSpeed() * 3.6) : "Speed: -";
//    bearingText = location.hasBearing() ? String.format("Bearing: %3.0f", location.getBearing()) : "Bearing: -";
//    accuracyText = location.hasAccuracy() ? String.format("Accuracy: %3.0f m", location.getAccuracy()) : "Accuracy: -";
//    timeText = String.format("Time: %s", new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(location.getTime()));

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

    tilePaint = new Paint(0);
  }
  
  public void setLayer(Layer layer) {
    
    this.layer = layer;
    
    invalidate();
  }
}
