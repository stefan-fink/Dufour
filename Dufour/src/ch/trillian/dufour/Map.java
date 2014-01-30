package ch.trillian.dufour;

public class Map {

  private final String name;
  private final Layer[] layers;
  
  public Map(String name, Layer[] layers) {
    
    this.name = name;
    this.layers = layers;
    
    // set layers map and indexes
    for (int layerIndex = 0; layerIndex < layers.length; layerIndex++) {
      layers[layerIndex].setMap(this);
      layers[layerIndex].setIndex(layerIndex);
    }
  }

  public int getLayerCount() {
    
    return layers.length;
  }
  
  public Layer getLayer(int layerIndex) {
    
    return layers[layerIndex];
  }
  
  public String getName() {
    return name;
  }

  public Layer[] getLayers() {
    return layers;
  }
}
