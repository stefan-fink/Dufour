package ch.trillian.dufour;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayDeque;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class TileLoader {

  private static final int LOADED_FROM_DB = 1;
  private static final int LOADED_FROM_URL = 2;
  private static final int LOAD_FAILED = 3;

  MapDatabaseHelper databaseHelper;
  LoadListener loadListener;
  Handler handler;
  ArrayDeque<Tile> mainDequeue;
  ArrayDeque<Tile> urlDequeue;

  Thread mainThread;
  Thread downloadThread;

  public interface LoadListener {

    public void onLoadFinished(Tile tile);
  }

  public TileLoader(Context context, Handler handler) {

    this.handler = handler;

    databaseHelper = new MapDatabaseHelper(context);
    mainDequeue = new ArrayDeque<Tile>();
    urlDequeue = new ArrayDeque<Tile>();

    Log.w("TRILLIAN", "TILES count=" + databaseHelper.getTileCount());
  }

  public void setLoadListener(LoadListener loadListener) {

    this.loadListener = loadListener;
  }

  public void testHandler() {

    Thread testThread = new Thread(new Runnable() {

      @Override
      public void run() {

        try {
          Thread.sleep(3000);
        } catch (Exception e) {
          Log.w("TRILLIAN", e.toString());
        }

        Message message = handler.obtainMessage(1234);
        message.sendToTarget();
      }
    });

    testThread.start();
  }

  public void orderLoadTile(Tile tile) {

    synchronized (mainDequeue) {

      mainDequeue.offer(tile);

      if (mainThread == null) {
        startMainThread();
      }
    }
  }

  public void cancelLoadTile(Tile tile) {

    synchronized (mainDequeue) {
      mainDequeue.remove(tile);
    }
  }

  private void startMainThread() {

    mainThread = new Thread(new Runnable() {

      @Override
      public void run() {

        while (true) {

          Tile tile = null;

          synchronized (mainDequeue) {
            if ((tile = mainDequeue.poll()) == null) {
              mainThread = null;
              Log.w("TRILLIAN", "MainThread stopped.");
              break;
            }
          }

          if (getTileFromDatabase(tile)) {
            continue;
          }

          getTileFromUrl(tile);
        }
      }
    });

    mainThread.start();
    Log.w("TRILLIAN", "MainThread started.");
  }

  private boolean getTileFromDatabase(Tile tile) {

    // read image from database
    byte[] image = databaseHelper.getTileImage(tile);
    if (image == null) {
      return false;
    }

    // decode image
    Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
    if (bitmap == null) {
      return false;
    }

    // set bitmap to tile
    tile.setBitmap(bitmap);
    handler.obtainMessage(LOADED_FROM_DB, tile).sendToTarget();

    return true;
  }

  private boolean getTileFromUrl(Tile tile) {

    try {

      // open http stream
      URL url = new URL(tile.getUrl());
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("referer", "http://map.geo.admin.ch/");
      InputStream inputStream = connection.getInputStream();

      // read inputStream into byte[]
      int numRead;
      byte[] block = new byte[16384];
      ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      while ((numRead = inputStream.read(block, 0, block.length)) != -1) {
        buffer.write(block, 0, numRead);
      }
      inputStream.close();
      connection.disconnect();
      buffer.flush();
      byte[] image = buffer.toByteArray();

      // convert byte[] to Bitmap
      Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
      if (bitmap != null) {
        tile.setBitmap(bitmap);
        handler.obtainMessage(LOADED_FROM_URL, tile).sendToTarget();
        databaseHelper.insertOrReplaceTileBitmap(tile, image);
        return true;
      }

    } catch (Exception e) {
      Log.w("TRILLIAN", "Exception: " + e.getMessage(), e);
      handler.obtainMessage(LOAD_FAILED, tile).sendToTarget();
    }

    return false;
  }
}
