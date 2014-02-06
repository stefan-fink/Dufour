package ch.trillian.dufour;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationProvider;
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
    
    public void preloadRegion(Layer layer, int minTileX, int maxTileX, int minTileY, int maxTileY);
    
    // public void gpsIsTracking(boolean isTracking);
  }
  
  // view listener (our activity)
  private ViewListener viewListener;
  
  private static final int INVALID_POINTER_ID = -1;

  // the map 
  private Layer layer;
  
  // attributes
  private int infoTextSize;
  private int infoTextColor;
  private int infoTextAltColor;
  private int infoLineColor;
  private int infoBackColor;
  private int infoLineStroke;
  private int textSize;
  private int gridStroke;
  private int gridTextSize;
  private int crossSize;
  private int crossStroke;

  // painters and paths
  private Paint infoPaint;
  private Paint textPaint;
  private Paint gridLinePaint;
  private Paint gridTextPaint;
  private Paint crossPaint;
  private Paint tilePaint;

  // bitmaps
  private Bitmap infoLocationBitmap;
  private Bitmap infoSpeedBitmap;
  private Bitmap infoAltitudeBitmap;
  
  // screen size in pixel
  private int screenSizeX;
  private int screenSizeY;

  // center position in pixel
  private float centerX;
  private float centerY;
  
  // position and zoom
  // pixelX = (positionX + x) * scale 
  // x = pixelX / scale - positionX
  private float positionX = 0f;
  private float positionY = 0f;
  private float scale = 1f;

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

  // GPS
  private boolean gpsEnabled;
  private boolean gpsTracking;
  private Location gpsLastLocation;
  private int gpsStatus;
  private String infoSpeed = "?";
  private String infoAltitude = "?";
  
  // true if info is displayed
  private boolean showInfo;

  public MapView(Context context, AttributeSet attrs) {

    super(context, attrs);

    // get attributes
    TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MapView, 0, 0);
    try {
      infoTextSize = a.getDimensionPixelSize(R.styleable.MapView_infoTextSize, 20);
      infoTextColor = a.getColor(R.styleable.MapView_infoTextColor, 0xFF000000);
      infoTextAltColor = a.getColor(R.styleable.MapView_infoTextAltColor, 0xFFFF0000);
      infoLineColor = a.getColor(R.styleable.MapView_infoLineColor, 0xFF000000);
      infoBackColor = a.getColor(R.styleable.MapView_infoBackColor, 0x80FFFFFF);
      infoLineStroke = a.getDimensionPixelSize(R.styleable.MapView_infoLineStroke, 1);
      textSize = a.getDimensionPixelSize(R.styleable.MapView_textSize, 10);
      gridStroke = a.getDimensionPixelSize(R.styleable.MapView_gridStroke, 1);
      gridTextSize = a.getDimensionPixelSize(R.styleable.MapView_gridTextSize, 10);
      crossSize = a.getDimensionPixelSize(R.styleable.MapView_crossSize, 10);
      crossStroke = a.getDimensionPixelSize(R.styleable.MapView_crossStroke, 1);
    } finally {
      a.recycle();
    }

    // init painters
    initPainters();

    // preload bitmaps
    infoLocationBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_info_location);
    infoSpeedBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_info_speed);
    infoAltitudeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_info_altitude);
    
    // Create our ScaleGestureDetector
    mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    mGestureDetector = new GestureDetector(context, new GestureListener());
  }

  public float getScale() {
    
    return scale;
  }
  
  public void setScale(float newScale) {
    
    scale(newScale / scale);
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
      if (gpsLastLocation != null) {
        setGpsTracking(true);
        setLocation(gpsLastLocation);
      }
      
      return true;
    }
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {

    float centerXnew = (float) w / 2f;
    float centerYnew = (float) h / 2f;
    
    positionX = (centerXnew - centerX + positionX * scale) / scale;
    positionY = (centerYnew - centerY + positionY * scale) / scale;

    screenSizeX = w;
    screenSizeY = h;
    
    centerX = centerXnew;
    centerY = centerYnew;
    
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
        if (gpsLastLocation != null && gpsTracking) {
          
          // calculate screen pixel coordinates of GPS location
          float[] mapPixel = new float[2];
          layer.locationToMapPixel(gpsLastLocation, mapPixel);
          float deltaX = map2screen(mapPixel[0], scale, positionX) - centerX;
          float deltaY = map2screen(mapPixel[1], scale, positionY) - centerY;
          float deltaSquare = deltaX * deltaX + deltaY * deltaY;
          float gpsTrackDistance = Math.min(screenSizeX, screenSizeX) / 10;
          if (deltaSquare > gpsTrackDistance * gpsTrackDistance) {
            setGpsTracking(false);
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
  
  @SuppressLint("DefaultLocale")
  protected void onDraw(Canvas canvas) {

    super.onDraw(canvas);
    
    canvas.save();
    canvas.scale(scale, scale);
    canvas.translate(positionX, positionY);

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
    if (gpsLastLocation != null) {
      canvas.save();
      float[] mapPixel = new float[2]; 
      layer.locationToMapPixel(gpsLastLocation, mapPixel);
      canvas.translate(mapPixel[0], mapPixel[1]);
      canvas.scale(1f/scale, 1f/scale);
      crossPaint.setColor(gpsTracking ? 0xFFFF0000 : 0xFF0000FF);
      crossPaint.setStyle(Paint.Style.FILL);
      canvas.drawCircle(0, 0, crossSize * 0.4f, crossPaint);
      crossPaint.setStyle(Paint.Style.STROKE);
      crossPaint.setColor(0xff000000);
      canvas.drawCircle(0, 0, crossSize * 0.4f, crossPaint);
      canvas.restore();
    }
    
    canvas.restore();

    // draw info
    if (showInfo) {
      
      float lineHeight = infoPaint.getFontSpacing() * 1.3f;
      
      // draw background
      y = 0f;
      int lines = gpsEnabled ? 2 : 1;
      infoPaint.setColor(infoBackColor);
      canvas.drawRect(0f, y, screenSizeX, lines * lineHeight, infoPaint);
     
      // draw coordinates
      String[] displayCoordinates = layer.getDisplayCoordinates(screen2map(centerX, scale, positionX), screen2map(centerY, scale, positionY));
      String text = String.format("%s, %s (%1.2f@%s)", displayCoordinates[0], displayCoordinates[1], scale, layer.getName());
      drawInfoText(canvas, infoLocationBitmap, text, 0f, y, screenSizeX, lineHeight, infoTextColor, infoPaint);

      // draw GPS details
      if (gpsLastLocation != null) {
        y += lineHeight;
        int textColor = gpsStatus == LocationProvider.AVAILABLE ? infoTextColor : infoTextAltColor;
        Log.w("TRILLIAN", String.format("color=%X", textColor));
        drawInfoText(canvas, infoSpeedBitmap, infoSpeed + " s=" + gpsStatus, 0f, y, centerX, lineHeight, textColor, infoPaint);
        infoPaint.setColor(infoLineColor);
        canvas.drawLine(centerX, y, centerX, y + lineHeight, infoPaint);
        drawInfoText(canvas, infoAltitudeBitmap, infoAltitude, centerX, y, centerX, lineHeight, textColor, infoPaint);
      }
    }
    
    // draw cross
    canvas.save();
    canvas.translate(centerX, centerY);
    canvas.drawCircle(0, 0, crossSize, crossPaint);
    canvas.drawLine(-crossSize, 0, crossSize, 0, crossPaint);
    canvas.drawLine(0, -crossSize, 0, crossSize, crossPaint);
    canvas.restore();
  }

  private void drawInfoText(Canvas canvas, Bitmap bitmap, String text, float x, float y, float width, float height, int textColor, Paint paint) {

    // draw bitmap
    canvas.save();
    paint.setColor(0xFF000000);
    float bitmapScale = height / bitmap.getHeight();
    canvas.scale(bitmapScale, bitmapScale);
    canvas.drawBitmap(bitmap, x, y / bitmapScale, paint);
    canvas.restore();
    
    // draw text
    paint.setColor(textColor);
    canvas.drawText(text, x + height + paint.descent(), y - paint.ascent() + 0.5f * (height - paint.getFontSpacing()), paint);
    
    // draw line
    paint.setColor(infoLineColor);
    canvas.drawLine(x, y + height, x + width, y + height, paint);
  }
  
  private void updateTilesMinMax() {
    
    // calculate new tile-region
    int minTileXnew = (int) Math.floor(-positionX / layer.getTileSizeX());
    int maxTileXnew = (int) Math.floor((screenSizeX / scale - positionX) / layer.getTileSizeX());
    int minTileYnew = (int) Math.floor(-positionY / layer.getTileSizeY());
    int maxTileYnew = (int) Math.floor((screenSizeY / scale - positionY) / layer.getTileSizeY());
    
    // remember new values and preload new region
    if (minTileXnew != minTileX || maxTileXnew != maxTileX || minTileYnew != minTileY || maxTileYnew != maxTileY) {
      
      // remember new values
      minTileX = minTileXnew;
      maxTileX = maxTileXnew;
      minTileY = minTileYnew;
      maxTileY = maxTileYnew;
      
      // preload tiles
      if (viewListener != null) {
        viewListener.preloadRegion(layer, minTileX, maxTileX, minTileY, maxTileY);
      }
    }
  }
  
  public void setLocation(Location location) {

    if (location == null) {
      return;
    }
    
    Log.w("TRILLIAN", String.format("setLocation: %f, %f", location.getLongitude(), location.getLatitude()));

    float[] mapPixel = new float[2];
    layer.locationToMapPixel(location, mapPixel);
    positionX = centerX / scale - mapPixel[0];
    positionY = centerY / scale - mapPixel[1];
    
    getLocation();
    
    invalidate();
  }

  public Location getLocation() {

    float mapPixelX = centerX / scale - positionX;
    float mapPixelY = centerY / scale - positionY;
    
    Location location = layer.mapPixel2location(mapPixelX, mapPixelY);
    
    Log.w("TRILLIAN", String.format("getLocation: %f, %f", location.getLongitude(), location.getLatitude()));

    return location;
  }

  public void setGpsLocation(Location location) {

    Log.w("TRILLIAN", "setGpsLocation() trackGps=" + gpsTracking);
    
    if (location == null) {
      gpsLastLocation = null;
      invalidate();
      return;
    }

    gpsLastLocation = location;
    
    double[] ch1903 = Ch1903.wgs84toCh1903(location);
    
    infoSpeed = gpsLastLocation.hasSpeed() ? String.format("%.1f km/h", gpsLastLocation.getSpeed() * 3.6f) : "- km/h";
    infoAltitude  = gpsLastLocation.hasAltitude() ? String.format("%.0f m", ch1903[2]) : "- km/h";


    // center map to gps position if we're tracking
    if (gpsTracking) {
      setLocation(gpsLastLocation);
    }

    invalidate();
  }

  public void setGpsEnabled(boolean enable) {
    
    gpsEnabled = enable;
  }
  
  public boolean isGpsEnabled() {
    
    return gpsEnabled;
  }
  
  public void setGpsTracking(boolean tracking) {
    
    gpsTracking = tracking;

    // center map to gps position if we're tracking
    if (tracking) {
      setLocation(gpsLastLocation);
    }

    invalidate();
  }
  
  public boolean isGpsTracking() {
    
    return gpsTracking;
  }
  
  public void setGpsStatus(int gpsStatus) {
    
    this.gpsStatus = gpsStatus;
    invalidate();
  }
  
  private void initPainters() {

    infoPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    infoPaint.setTextAlign(Paint.Align.LEFT);
    infoPaint.setTextSize(infoTextSize);
    infoPaint.setStrokeWidth(infoLineStroke);
    
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
  
  public Layer getLayer() {
    
    return layer;
  }
  
  public void setLayer(Layer layer) {
    
    this.layer = layer;
    
    invalidate();
  }

  public boolean isShowInfo() {
  
    return showInfo;
  }
  
  public void setShowInfo(boolean showInfo) {
    
    this.showInfo = showInfo;
    
    invalidate();
  }
}
