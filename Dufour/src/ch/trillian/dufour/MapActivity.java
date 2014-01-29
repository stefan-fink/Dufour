package ch.trillian.dufour;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;

import ch.trillian.dufour.R;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.Menu;

public class MapActivity extends Activity implements MapView.ViewListener {

  private final Map ch1903Map = createMap("ch1903");
  private MapView mapView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    mapView = (MapView) findViewById(R.id.map_view);
    mapView.setMap(ch1903Map, 2);
    mapView.setViewListener(this);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.map, menu);
    return true;
  }

  private Map createMap(String mapName) {

    String urlFormat = "http://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/20140106/21781/%1$s/%3$d/%2$d.jpeg";

    Layer[] layers = { 
        new Layer("CH1903-16", "16", urlFormat, 256, 256, 0, 0,    7,    4, 0.5f, 3.0f),
        new Layer("CH1903-17", "17", urlFormat, 256, 256, 0, 0,   18,   12, 0.5f, 3.0f),
        new Layer("CH1903-18", "18", urlFormat, 256, 256, 0, 0,   37,   24, 0.5f, 3.0f), // 50m/pixel
        new Layer("CH1903-19", "19", urlFormat, 256, 256, 0, 0,   93,   62, 0.5f, 3.0f),
        new Layer("CH1903-20", "20", urlFormat, 256, 256, 0, 0,  187,  124, 0.5f, 3.0f),
        new Layer("CH1903-21", "21", urlFormat, 256, 256, 0, 0,  374,  249, 0.5f, 3.0f),
        new Layer("CH1903-22", "22", urlFormat, 256, 256, 0, 0,  749,  499, 0.5f, 3.0f),
        new Layer("CH1903-23", "23", urlFormat, 256, 256, 0, 0,  937,  624, 0.5f, 3.0f),
        new Layer("CH1903-24", "24", urlFormat, 256, 256, 0, 0, 1249,  833, 0.5f, 3.0f),
        new Layer("CH1903-25", "25", urlFormat, 256, 256, 0, 0, 1875, 1249, 0.5f, 3.0f)
    };

    return new Map(1, mapName, layers);
  }

  @Override
  public void onOrderLoadTile(Tile tile) {

    Log.w("TRILLIAN", "onOrderLoadTile: " + tile);
    
    new DownloadMapTask().execute(tile);
  }

  @Override
  public void onCancelLoadTile(Tile tile) {

    Log.w("TRILLIAN", "onCancelLoadTile: " + tile);
  }

  private class DownloadMapTask extends AsyncTask<Tile, Integer, Tile> {

    protected Tile doInBackground(Tile... tiles) {

      Tile tile = tiles[0];

      try {

        // URL("http://wmts.geo.admin.ch/1.0.0/ch.swisstopo.pixelkarte-farbe/default/20140106/21781/19/7/12.jpeg");

        Layer layer = tile.getMap().getLayer(tile.getLayerIndex());
        URL url = new URL(layer.getUrl(tile));

        Log.w("TRILLIAN", url.toString());

         HttpURLConnection connection = (HttpURLConnection)
         url.openConnection();
         connection.addRequestProperty("referer", "http://map.geo.admin.ch/");
         InputStream inputStream = connection.getInputStream();
        
         tile.setBitmap(BitmapFactory.decodeStream(inputStream));
        
         inputStream.close();
         connection.disconnect();
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
        mapView.setTile(tile);
      }
    }
  }
}
