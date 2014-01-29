package ch.trillian.dufour;

public class Map {

  private final int mapId;
  private final String mapName;
  private final Layer[] layers;
  
  public Map(int mapId, String mapName, Layer[] layers) {
    
    this.mapId = mapId;
    this.mapName = mapName;
    this.layers = layers;
  }

  public int getLayerCount() {
    
    return layers.length;
  }
  
  public Layer getLayer(int layerIndex) {
    
    return layers[layerIndex];
  }
  
  public int getMapId() {
    return mapId;
  }

  public String getMapName() {
    return mapName;
  }

  public Layer[] getLayers() {
    return layers;
  }
}
