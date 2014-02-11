package ch.trillian.dufour;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

public class Tile {

  private final Layer layer; 
  private final int x;
  private final int y;
  private long lastUsed;
  private Bitmap bitmap;
  private int state;
  
  public Tile(Layer layer, int x, int y) {
    
    this.layer = layer;
    this.x = x;
    this.y = y;
  }

  @SuppressLint("DefaultLocale")
  public String toString() {
    
    return String.format("layer=%s, x=%d, y=%d",  layer.getName(), x, y);
  }
  
  public String getUrl() {
    
    return layer.getUrl(this);
  }
  
  public Layer getLayer() {
    return layer;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  public boolean isLoading() {
    return bitmap == null;
  }

  public Bitmap getBitmap() {
    return bitmap;
  }
  
  public void setBitmap(Bitmap bitmap) {
    this.bitmap = bitmap;
  }
  
  public long getLastUsed() {
    return lastUsed;
  }

  public void setLastUsed(long lastUsed) {
    this.lastUsed = lastUsed;
  }

  public int getState() {
    return state;
  }

  public void setState(int state) {
    this.state = state;
  }
}
