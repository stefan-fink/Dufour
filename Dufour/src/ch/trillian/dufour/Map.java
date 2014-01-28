package ch.trillian.dufour;

public class Map {

  private final String mapName;
  private final Layer[] layers;
  
  public Map(String mapName, Layer[] layers) {
    
    this.mapName = mapName;
    this.layers = layers;
  }

  public String getMapName() {
    return mapName;
  }

  public Layer[] getLayers() {
    return layers;
  }
}
