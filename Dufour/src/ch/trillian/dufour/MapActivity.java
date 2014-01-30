package ch.trillian.dufour;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

public class MapActivity extends Activity {

  private final Map map = createMap();
  private MapView mapView;
  private TileCache tileCache;
  private MapDatabaseHelper databaseHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    
    super.onCreate(savedInstanceState);

    // init view
    setContentView(R.layout.activity_map);
    mapView = (MapView) findViewById(R.id.map_view);
    mapView.setLayer(map.getLayer(0));
    mapView.setViewListener(new MapViewListener());
    
    // init database
    databaseHelper = new MapDatabaseHelper(this);
    
    Log.w("TRILLIAN", "TILES count=" + databaseHelper.getTileCount());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.map, menu);
    return true;
  }

  private Map createMap() {

    String urlFormat = "http://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/20140106/21781/%1$s/%3$d/%2$d.jpeg";

    Layer[] layers = { 
        new Layer("16", urlFormat, 256, 256, 0, 0,    7,    4, 0.5f, 3.0f),
        new Layer("17", urlFormat, 256, 256, 0, 0,   18,   12, 0.5f, 3.0f),
        new Layer("18", urlFormat, 256, 256, 0, 0,   37,   24, 0.5f, 3.0f), // 50m/pixel
        new Layer("19", urlFormat, 256, 256, 0, 0,   93,   62, 0.5f, 3.0f),
        new Layer("20", urlFormat, 256, 256, 0, 0,  187,  124, 0.5f, 3.0f),
        new Layer("21", urlFormat, 256, 256, 0, 0,  374,  249, 0.5f, 3.0f),
        new Layer("22", urlFormat, 256, 256, 0, 0,  749,  499, 0.5f, 3.0f),
        new Layer("23", urlFormat, 256, 256, 0, 0,  937,  624, 0.5f, 3.0f),
        new Layer("24", urlFormat, 256, 256, 0, 0, 1249,  833, 0.5f, 3.0f),
        new Layer("25", urlFormat, 256, 256, 0, 0, 1875, 1249, 0.5f, 3.0f)
    };

    return new Map("CH1903-25", layers);
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
  
  private class CacheListener implements TileCache.CacheListener {

    @Override
    public void onOrderLoadTile(Tile tile) {
      
      Log.w("TRILLIAN", "onOrderLoadTile: " + tile);
      new DownloadMapTask().execute(tile);
    }

    @Override
    public void onCancelLoadTile(Tile tile) {
      
      Log.w("TRILLIAN", "onCancelLoadTile: " + tile);
    }
  }

  private class DownloadMapTask extends AsyncTask<Tile, Integer, Tile> {

    protected Tile doInBackground(Tile... tiles) {

      Tile tile = tiles[0];

      try {

        byte[] image = databaseHelper.getTileImage(tile);
        
        if (image != null) {
          Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
          if (bitmap != null) {
            tile.setBitmap(bitmap);
            Log.w("TRILLIAN", "Tile loaded from DB: " + tile);
            return tile;
          }
        }
        
        // URL("http://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/20140106/21781/19/7/12.jpeg");

        URL url = new URL(tile.getUrl());

        Log.w("TRILLIAN", url.toString());

        HttpURLConnection connection = (HttpURLConnection)
        url.openConnection();
        connection.addRequestProperty("referer", "http://map.geo.admin.ch/");
        InputStream inputStream = connection.getInputStream();
        
        // convert inputStream to byte[]
        int numRead;
        byte[] block = new byte[16384];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while ((numRead = inputStream.read(block, 0, block.length)) != -1) {
          buffer.write(block, 0, numRead);
        }
        inputStream.close();
        connection.disconnect();
        buffer.flush();
        image = buffer.toByteArray();

        // convert to Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
        if (bitmap != null) {
          Log.w("TRILLIAN", "Tile loaded from URL: " + tile);
          tile.setBitmap(bitmap);
          databaseHelper.insertOrReplaceTileBitmap(tile, image);
        }
        
      } catch (Exception e) {
        Log.w("TRILLIAN", "Exception: " + e.getMessage(), e);
      }

      return tile;
    }

    protected void onPostExecute(Tile tile) {

      if (tile == null) {
        Log.w("TRILLIAN", "tile: null");
      } else if (tile.getBitmap() == null) {
        Log.w("TRILLIAN", "tile.bitmap: null");
      } else {
        Log.w("TRILLIAN", "tile.bitmap: (" + tile.getBitmap().getWidth() + ", " + tile.getBitmap().getHeight() +")");
        tileCache.setTile(tile);
        mapView.invalidate();
      }
    }
  }
}
