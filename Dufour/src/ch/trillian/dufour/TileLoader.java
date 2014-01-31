package ch.trillian.dufour;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

public class TileLoader {

  MapDatabaseHelper databaseHelper;
  LoadListener loadListener;
  ArrayDeque<Tile> dequeue;
  DownloadTask downloaTask;

  public interface LoadListener {

    public void onLoadFinished(Tile tile);
  }

  public TileLoader(Context context) {

    databaseHelper = new MapDatabaseHelper(context);
    dequeue = new ArrayDeque<Tile>();

    Log.w("TRILLIAN", "TILES count=" + databaseHelper.getTileCount());
  }

  public void setLoadListener(LoadListener loadListener) {

    this.loadListener = loadListener;
  }

  public void orderLoadTile(Tile tile) {

    synchronized (dequeue) {
      dequeue.offer(tile);
      if (downloaTask == null) {
        downloaTask = new DownloadTask();
        downloaTask.execute(tile);
        Log.w("TRILLIAN", "DownloadTask started.");
      }
    }
  }

  public void cancelLoadTile(Tile tile) {

    synchronized (dequeue) {
      dequeue.remove(tile);
    }
  }

  private class DownloadTask extends AsyncTask<Object, Tile, Object> {

    protected Object doInBackground(Object... objects) {

      try {

        while (true) {

          Tile tile;

          synchronized (dequeue) {
            if ((tile = dequeue.poll()) == null) {
              return downloaTask = null;
            }
          }

          byte[] image = databaseHelper.getTileImage(tile);

          if (image != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            if (bitmap != null) {
              // Log.w("TRILLIAN", "Tile loaded from DB: " + tile);
              tile.setBitmap(bitmap);
              publishProgress(tile);
              continue;
            }
          }

          URL url = new URL(tile.getUrl());
          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
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
            // Log.w("TRILLIAN", "Tile loaded from URL: " + tile);
            tile.setBitmap(bitmap);
            publishProgress(tile);
            databaseHelper.insertOrReplaceTileBitmap(tile, image);
          }
        }

      } catch (Exception e) {
        Log.w("TRILLIAN", "Exception: " + e.getMessage(), e);
      }

      synchronized (dequeue) {
        return downloaTask = null;
      }
    }

    @Override
    protected void onProgressUpdate(Tile... tiles) {

      if (loadListener != null) {
        loadListener.onLoadFinished(tiles[0]);
      }
    }

    protected void onPostExecute(Object result) {

    }
  }
}
