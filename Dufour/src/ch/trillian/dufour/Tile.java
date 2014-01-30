package ch.trillian.dufour;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

public class Tile {

  private final Map map; 
  private final Layer layer; 
  private final int x;
  private final int y;
  private Bitmap bitmap;
  
  public Tile(Map map, Layer layer, int x, int y) {
    
    this.map = map;
    this.layer = layer;
    this.x = x;
    this.y = y;
  }

  @SuppressLint("DefaultLocale")
  public String toString() {
    
    return String.format("mapName=%s, layerName=%s, x=%d, y=%d",  map.getName(), layer.getName(), x, y);
  }
  
  public String getUrl() {
    
    return layer.getUrl(this);
  }
  
  public Map getMap() {
    return map;
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
}
