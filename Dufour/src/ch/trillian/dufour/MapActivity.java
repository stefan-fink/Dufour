package ch.trillian.dufour;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

public class MapActivity extends Activity {

  private final Map map = createMap();
  private MapView mapView;
  private TileCache tileCache;
  private TileLoader tileLoader;
  
  // true if GPS is enables
  boolean gpsIsEnabled;
  
  // our optionMenu
  private Menu optionMenu;

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
    optionMenu = menu;
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    switch (item.getItemId()) {
    case R.id.action_gps:
      gpsIsEnabled = !gpsIsEnabled;
      enableGps(gpsIsEnabled);
      return true;
    }
    
    return super.onOptionsItemSelected(item);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {

    int action = event.getAction();
    int keyCode = event.getKeyCode();
    
    switch (keyCode) {
    
    case KeyEvent.KEYCODE_VOLUME_UP:
      if (action == KeyEvent.ACTION_DOWN) {
        mapView.scale(1.3f);
      }
      return true;
      
    case KeyEvent.KEYCODE_VOLUME_DOWN:
      if (action == KeyEvent.ACTION_DOWN) {
        mapView.scale(1/1.3f);
      }
      return true;
      
    default:
      return super.dispatchKeyEvent(event);
    }
  }
  
  @Override
  protected void onPause() {

    if (gpsIsEnabled) {
      enableGps(false);
    }

    super.onPause();
  }

  @Override
  protected void onResume() {

    super.onResume();

    if (gpsIsEnabled) {
      enableGps(true);
    }
  }

  private final void enableGps(boolean start) {
    
    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    if (start) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
      Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      mapView.setGpsLocation(location);
    } else {
      locationManager.removeUpdates(locationListener);
      mapView.setGpsLocation(null);
    }
    
    // change GPS icon
    if (optionMenu != null) {
      MenuItem actionGps = optionMenu.findItem(R.id.action_gps);
      if (actionGps != null) {
        actionGps.setIcon(start ? R.drawable.ic_action_gps_off : R.drawable.ic_action_gps);
      }
    }
  }
  
  private final LocationListener locationListener = new LocationListener() {

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onLocationChanged(Location location) {

      mapView.setGpsLocation(location);
    }
  };

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
