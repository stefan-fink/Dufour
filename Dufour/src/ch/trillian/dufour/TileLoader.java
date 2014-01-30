package ch.trillian.dufour;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class TileLoader {
  
  MapDatabaseHelper databaseHelper;
  LoadListener loadListener;
  
  public interface LoadListener {
  
    public void onLoadFinished(Tile tile);
  }
  
  public TileLoader(Context context) {
  
    databaseHelper = new MapDatabaseHelper(context);

    Log.w("TRILLIAN", "TILES count=" + databaseHelper.getTileCount());
  }
  
  public void setLoadListener(LoadListener loadListener) {
    
    this.loadListener = loadListener;
  }
  
  public void orderLoadTile(Tile tile) {
    
    new DownloadTask().execute(tile);
  }
  
  public void cancelLoadTile(Tile tile) {
    
  }
  
  private class DownloadTask extends AsyncTask<Tile, Integer, Tile> {

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

      if (loadListener != null) {
        loadListener.onLoadFinished(tile);
      }
    }
  }
}
