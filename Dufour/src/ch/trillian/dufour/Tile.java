package ch.trillian.dufour;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

public class Tile {

  private final Map map; 
  private final int layerIndex ;
  private final int x;
  private final int y;
  private Bitmap bitmap;
  
  public Tile(Map map, int layerIndex, int x, int y) {
    
    this.map = map;
    this.layerIndex = layerIndex;
    this.x = x;
    this.y = y;
  }

  @SuppressLint("DefaultLocale")
  public String toString() {
    
    return String.format("mapId=%d, layerIndex=%d, x=%d, y=%d",  map.getMapId(), layerIndex, x, y);
  }
  
  public Map getMap() {
    return map;
  }

  public int getLayerIndex() {
    return layerIndex;
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
