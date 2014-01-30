package ch.trillian.dufour;

import android.util.Log;

public class TileCache {

  public final static int PRELOAD_SIZE = 1;
  
  private Map map;
  private Tile[][][] cache;
  private CacheListener cacheListener; 
  
  public interface CacheListener {
    
    public void onOrderLoadTile(Tile tile);

    public void onCancelLoadTile(Tile tile);
  }
  
  public TileCache(Map map, int preloadSize, int screenSizeX, int screenSizeY) {
    
    this.map = map;
    
    // create array of layers
    cache = new Tile[map.getLayerCount()][][];
    
    for (int layerIndex = 0; layerIndex < map.getLayerCount(); layerIndex++) {
      
      Layer layer = map.getLayer(layerIndex);
      int cacheSizeX = (int) ( (1f / layer.getMinScale()) * screenSizeX / layer.getTileSizeX()) + 2 * preloadSize + 1;
      int cacheSizeY = (int) ( (1f / layer.getMinScale()) * screenSizeY / layer.getTileSizeY()) + 2 * preloadSize + 1;
      
      cacheSizeX = Math.min(cacheSizeX, layer.getSizeX());
      cacheSizeY = Math.min(cacheSizeY, layer.getSizeY());
      
      // create array of columns
      cache[layerIndex] = new Tile[cacheSizeY][];
      
      // create arrays of rows
      for (int y = 0; y < cacheSizeY; y++) {
        cache[layerIndex][y] = new Tile[cacheSizeX];
      }
      
      Log.w("TRILLIAN", "Created cache: mapId=" + map.getMapId() + ", layerIndex=" + layerIndex + ", cacheSizeX="+ cacheSizeX + ", cacheSizeY="+ cacheSizeY);
    }
  }
  
  public void setCacheListener(CacheListener cacheListener) {
    
    this.cacheListener = cacheListener;
  }
  
  public Tile getTile(Map map, int layerIndex, int x, int y) {
    
    if (this.map != map) {
      return null;
    }
    
    if (!map.getLayer(layerIndex).hasTile(x, y)) {
      return null;
    }
    
    int cacheSizeX = cache[layerIndex][0].length;
    int cacheSizeY = cache[layerIndex].length;
    
    int cacheIndexX = x % cacheSizeX;
    int cacheIndexY = y % cacheSizeY;

    Tile tile = cache[layerIndex][cacheIndexY][cacheIndexX];
    
    // check if current cached item matches the requested one
    if (tile != null) {
      if (tile.getX() != x || tile.getY() != y) {
        if (tile.isLoading()) {
          cancelLoad(tile);
        }
        tile = null;
      }
    }
    
    // order new tile if none exists
    if (tile == null) {
      tile = new Tile(map, layerIndex, x, y);
      cache[layerIndex][cacheIndexY][cacheIndexX] = tile;
      orderLoad(tile);
    }

    return tile;
  }
  
  public void setTile(Tile tile) {

    if (map != tile.getMap()) {
      return;
    }
    
    int x = tile.getX();
    int y = tile.getY();
    
    if (!map.getLayer(tile.getLayerIndex()).hasTile(x, y)) {
      return;
    }

    int layerIndex = tile.getLayerIndex();
    int cacheSizeX = cache[layerIndex][0].length;
    int cacheSizeY = cache[layerIndex].length;
    
    int cacheIndexX = x % cacheSizeX;
    int cacheIndexY = y % cacheSizeY;

    Tile currentTile = cache[layerIndex][cacheIndexY][cacheIndexX];
    
    if (currentTile != null && currentTile.isLoading()) {
      cancelLoad(tile);
    }
    
    cache[layerIndex][cacheIndexY][cacheIndexX] = tile;
  }
  
  private void orderLoad(Tile tile) {
    
    if (cacheListener != null) {
      cacheListener.onOrderLoadTile(tile);
    }
  }
  
  private void cancelLoad(Tile tile) {
    
    if (cacheListener != null) {
      cacheListener.onCancelLoadTile(tile);
    }
  }
}
