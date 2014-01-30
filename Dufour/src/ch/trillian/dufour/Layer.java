package ch.trillian.dufour;

public class Layer {

  private final String name; 
  private final String urlFormat; 
  private final int tileSizeX;
  private final int tileSizeY;
  private final int left;
  private final int top;
  private final int right;
  private final int bottom;
  private final int sizeX;
  private final int sizeY;
  private final float minScale;
  private final float maxScale;
  
  private Map map;
  private int index;
  
  public Layer(String name, String urlFormat, int tileSizeX, int tileSizeY, int left, int top, int right, int bottom, float minScale, float maxScale) {
    
    this.name = name;
    this.urlFormat = urlFormat;
    this.tileSizeX = tileSizeX;
    this.tileSizeY = tileSizeY;
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
    this.minScale = minScale;
    this.maxScale = maxScale;
    
    sizeX = right > left ? right - left + 1 : left - right + 1; 
    sizeY = bottom > top ? bottom - top + 1 : top - bottom + 1; 
  }

  public Map getMap() {
    
    return map;
  }
  
  public void setMap(Map map) {
    
    this.map = map;
  }
  
  public int getIndex() {
    
    return index;
  }
  
  public void setIndex(int layerIndex) {
    
    this.index = layerIndex;
  }
  
  public String getUrl(Tile tile) {
    
    return String.format(urlFormat, name, getUrlX(tile.getX()), getUrlY(tile.getY()));
  }
  
  public int getUrlX(int x) {
    
    return left < right ? left + x : left - x;
  }
  
  public int getUrlY(int y) {
    
    return top < bottom ? top + y : top - y;
  }
  
  public boolean hasTile(int x, int y) {

    return x >= 0 && x < sizeX && y >= 0 && y < sizeY;
  }
  
  public String getName() {
    return name;
  }

  public int getTileSizeX() {
    return tileSizeX;
  }

  public int getTileSizeY() {
    return tileSizeY;
  }

  public int getLeft() {
    return left;
  }

  public int getTop() {
    return top;
  }

  public int getRight() {
    return right;
  }

  public int getBottom() {
    return bottom;
  }

  public int getSizeX() {
    return sizeX;
  }

  public int getSizeY() {
    return sizeY;
  }

  public float getMinScale() {
    return minScale;
  }

  public float getMaxScale() {
    return maxScale;
  }
}
