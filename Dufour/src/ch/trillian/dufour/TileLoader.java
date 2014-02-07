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
import android.util.Log;

public class TileLoader {

  private static final int LOADED_FROM_DB = 1;
  private static final int LOADED_FROM_URL = 2;
  private static final int LOAD_FAILED = 3;

  MapDatabaseHelper databaseHelper;
  LoadListener loadListener;
  Handler handler;
  ArrayDeque<Tile> databaseDequeue;
  ArrayDeque<Tile> downloadDequeue;

  DatabaseThread databaseThread;
  DownloadThread downloadThread;
  
  boolean stopping;

  public interface LoadListener {

    public void onLoadFinished(Tile tile);
  }

  public TileLoader(Context context) {

    databaseHelper = new MapDatabaseHelper(context);

    Log.w("TRILLIAN", "TILES count=" + databaseHelper.getTileCount());
  }

  public void start(Handler handler) {
    
    this.handler = handler;

    databaseDequeue = new ArrayDeque<Tile>();
    downloadDequeue = new ArrayDeque<Tile>();
  }
  
  public void stop() {

    stopping = true;
    
    if (databaseThread != null) {
      databaseThread.shutdown();
      databaseThread = null;
    }
    
    if (downloadThread != null) {
      downloadThread.shutdown();
      downloadThread = null;
    }
    
    databaseDequeue = null;
    downloadDequeue = null;
  }
  
  public void setLoadListener(LoadListener loadListener) {

    this.loadListener = loadListener;
  }

  public void orderLoadTile(Tile tile) {

    synchronized (databaseDequeue) {

      // put tile in order queue
      databaseDequeue.offer(tile);

      // (re)start thread
      if (databaseThread == null) {
        databaseThread = new DatabaseThread();
        databaseThread.start();
      }
      
      if (databaseDequeue.size() == 1) {
        databaseDequeue.notify();
      }
    }
  }

  public void cancelLoadTile(Tile tile) {

    synchronized (databaseDequeue) {
      databaseDequeue.remove(tile);
    }
    
    synchronized (downloadDequeue) {
      downloadDequeue.remove(tile);
    }
  }

  private class DatabaseThread extends Thread {
  
    private boolean shutdown;
    
    public void shutdown() {
      
      shutdown = true;
      synchronized (databaseDequeue) {
        databaseDequeue.notify();
      }
    }
    
    public void run() {

      Log.w("TRILLIAN", "DatabaseThread started.");

      int numTiles = 0;
      
      try {
        
        while (true) {
  
          if (shutdown) {
            Log.w("TRILLIAN", "DatabaseThread has been shut down.");
            return;
          }
          
          Tile tile = null;
  
          synchronized (databaseDequeue) {
            if ((tile = databaseDequeue.poll()) == null) {
              Log.w("TRILLIAN", "DatabaseThread going to sleep (loaded " + numTiles + " tiles).");
              databaseDequeue.wait();
              Log.w("TRILLIAN", "DatabaseThread woke up.");
              numTiles = 0;
              continue;
            }
          }
  
          if (getTileFromDatabase(tile)) {
            numTiles++;
            continue;
          }
  
          // order tile from download thread
          orderDownloadTile(tile);
        }
        
      } catch (InterruptedException e) {
        synchronized (databaseDequeue) {
          Log.w("TRILLIAN", "DatabaseThread has been interrupted.");
          databaseThread = null;
        }
      }
    }
  }
  
  private void orderDownloadTile(Tile tile) {

    synchronized (downloadDequeue) {

      // put tile in order queue
      downloadDequeue.offer(tile);

      // (re)start thread
      if (downloadThread == null) {
        downloadThread = new DownloadThread();
        downloadThread.start();
      }
      
      if (downloadDequeue.size() == 1) {
        downloadDequeue.notify();
      }
    }
  }

  private class DownloadThread extends Thread {
    
    private boolean shutdown;
    
    public void shutdown() {
      
      shutdown = true;
      synchronized (downloadDequeue) {
        downloadDequeue.notify();
      }
    }
    
    public void run() {

      Log.w("TRILLIAN", "DownloadThread started.");
      
      int numTiles = 0;

      try {
        
        while (true) {
  
          if (shutdown) {
            Log.w("TRILLIAN", "DownloadThread has been shut down.");
            return;
          }
          
          Tile tile = null;
  
          synchronized (downloadDequeue) {
            if ((tile = downloadDequeue.poll()) == null) {
              Log.w("TRILLIAN", "DownloadThread going to sleep (downloaded " + numTiles + " tiles).");
              downloadDequeue.wait();
              Log.w("TRILLIAN", "DownloadThread woke up.");
              numTiles = 0;
              continue;
            }
          }
  
          getTileFromUrl(tile);
          
          numTiles++;
        }
        
      } catch (InterruptedException e) {
        synchronized (downloadDequeue) {
          Log.w("TRILLIAN", "DownloadThread has been interrupted.");
          downloadThread = null;
        }
      }
    }
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
