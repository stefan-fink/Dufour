package ch.trillian.dufour;

public class Layer {

  private final String layerName; 
  private final int tileSizeX;
  private final int tileSizeY;
  private final int maxTileX;
  private final int maxTileY;
  
  public Layer(String layerName, int tileSizeX, int tileSizeY, int maxTileX, int maxTileY) {
    
    this.layerName = layerName;
    this.tileSizeX = tileSizeX;
    this.tileSizeY = tileSizeY;
    this.maxTileX = maxTileX;
    this.maxTileY = maxTileY;
  }

  public String getLayerName() {
    return layerName;
  }

  public int getTileSizeX() {
    return tileSizeX;
  }

  public int getTileSizeY() {
    return tileSizeY;
  }

  public int getMaxTileX() {
    return maxTileX;
  }

  public int getMaxTileY() {
    return maxTileY;
  }
}
