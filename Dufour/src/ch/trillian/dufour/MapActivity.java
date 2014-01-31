package ch.trillian.dufour;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MapActivity extends Activity {

  private final Map map = createMap();
  private MapView mapView;
  private TileCache tileCache;
  private TileLoader tileLoader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    
    super.onCreate(savedInstanceState);

    // init loader
    tileLoader = new TileLoader(this);
    tileLoader.setLoadListener(new LoadListener());

    // init view
    setContentView(R.layout.activity_map);
    mapView = (MapView) findViewById(R.id.map_view);
    mapView.setLayer(map.getLayer(0));
    mapView.setViewListener(new MapViewListener());
    
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.map, menu);
    return true;
  }

  private Map createMap() {

    String urlFormat = "http://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/20140106/21781/%1$s/%3$d/%2$d.jpeg";

    Layer[] layers = { 
        new Layer("16", urlFormat, 420000f, 350000f, 250f, 256, 256, 0, 0,    7,    4),
        new Layer("17", urlFormat, 420000f, 350000f, 100f, 256, 256, 0, 0,   18,   12),
        new Layer("18", urlFormat, 420000f, 350000f,  50f, 256, 256, 0, 0,   37,   24),
        new Layer("19", urlFormat, 420000f, 350000f,  20f, 256, 256, 0, 0,   93,   62),
        new Layer("20", urlFormat, 420000f, 350000f,  10f, 256, 256, 0, 0,  187,  124),
        new Layer("21", urlFormat, 420000f, 350000f,   5f, 256, 256, 0, 0,  374,  249),
        // new Layer("22", urlFormat, 420000f, 350000f, 2.5f, 256, 256, 0, 0,  749,  499),
        new Layer("23", urlFormat, 420000f, 350000f, 2.0f, 256, 256, 0, 0,  937,  624),
        // new Layer("24", urlFormat, 420000f, 350000f, 1.5f, 256, 256, 0, 0, 1249,  833),
        new Layer("25", urlFormat, 420000f, 350000f, 1.0f, 256, 256, 0, 0, 1875, 1249),
        //new Layer("26", urlFormat, 420000f, 350000f, 0.5f, 256, 256, 0, 0, 3749, 2499),
    };

    return new Map("CH1903-25", layers, 0.5f, 10.0f, 1.5f, 1.5f);
  }

  private class MapViewListener implements MapView.ViewListener {

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {

      if (w == 0 || h == 0) {
        return;
      }

      Log.w("TRILLIAN", "onSizeChanged: " + (tileCache == null ? "no old cache" : "has old cache"));

      tileCache = new TileCache(map, TileCache.PRELOAD_SIZE, w, h);
      tileCache.setCacheListener(new CacheListener());
    }

    @Override
    public Tile onGetTile(Layer layer, int x, int y) {
      
      if (tileCache == null) {
        return null;
      }
      
      return tileCache.getTile(layer, x, y);
    }
  }
  
  private class LoadListener implements TileLoader.LoadListener {

    @Override
    public void onLoadFinished(Tile tile) {
      
      if (tile == null) {
        Log.w("TRILLIAN", "tile: null");
      } else if (tile.getBitmap() == null) {
        Log.w("TRILLIAN", "tile.bitmap: null" + tile);
      } else {
        tileCache.setTile(tile);
        mapView.invalidate();
      }
    }
  }
  
  private class CacheListener implements TileCache.CacheListener {

    @Override
    public void onOrderLoadTile(Tile tile) {
      
      // Log.w("TRILLIAN", "onOrderLoadTile: " + tile);
      tileLoader.orderLoadTile(tile);
    }

    @Override
    public void onCancelLoadTile(Tile tile) {
      
      // Log.w("TRILLIAN", "onCancelLoadTile: " + tile);
      tileLoader.cancelLoadTile(tile);
    }
  }
}
