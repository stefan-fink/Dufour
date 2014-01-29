package ch.trillian.dufour;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

public class Tile {

  private final int mapId; 
  private final int layerIndex;
  private final int x;
  private final int y;
  private Bitmap bitmap;
  
  public Tile(int mapId, int layerIndex, int x, int y) {
    
    this.mapId = mapId;
    this.layerIndex = layerIndex;
    this.x = x;
    this.y = y;
  }

  @SuppressLint("DefaultLocale")
  public String toString() {
    
    return String.format("mapId=%d, layerIndex=%d, x=%d, y=%d",  mapId, layerIndex, x, y);
  }
  
  public int getMapId() {
    return mapId;
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
}
