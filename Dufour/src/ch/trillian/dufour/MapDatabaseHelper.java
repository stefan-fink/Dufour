package ch.trillian.dufour;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class MapDatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "map.db";
  private static final int DATABASE_VERSION = 2;

  // our database
  SQLiteDatabase db;
  
  // the actual number of tiles
  private int tileCount;
  
  public MapDatabaseHelper(Context context) {
    
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    
    TileTable.onCreate(database);
  }

  @Override
  public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    
    TileTable.onUpgrade(database, oldVersion, newVersion);
  }
  
  public void open() {
    
    db = getWritableDatabase();
    tileCount = readTileCount();
    Log.w("TRILLIAN", "open() Tilecount=" + tileCount);
  }

  private static final String SQL_GET_TILE_COUNT = "SELECT COUNT(*) FROM " + TileTable.TABLE_NAME;

  public int getTileCount() {
    
    return tileCount;
  }
  
  public int readTileCount() {
    
    long start = System.currentTimeMillis();
    
    Log.w("TRILLIAN", "getTileCount() getReadableDatabase=" + (System.currentTimeMillis() - start));
    
    Cursor cursor = db.rawQuery(SQL_GET_TILE_COUNT, new String[] {});
    
    if (cursor == null) {
      return -1;
      
    }

    try {
      if(cursor.moveToFirst()) {
        return cursor.getInt(0);
      }
    } finally {
      cursor.close();
    }

    return -1;
  }
  
  private static final String SQL_GET_TILE_IMAGE = "SELECT " + TileTable.COL_LAST_USED + ", " + TileTable.COL_IMAGE + " FROM " + TileTable.TABLE_NAME + " WHERE " + TileTable.COL_MAP_ID + " = ? AND " + TileTable.COL_LAYER_ID + " = ? AND " + TileTable.COL_X + " = ? AND " + TileTable.COL_Y + "=?";

  public synchronized boolean getTileImage(Tile tile) {

    Cursor cursor = db.rawQuery(SQL_GET_TILE_IMAGE, new String[] { tile.getMap().getName(), tile.getLayer().getName(), String.valueOf(tile.getX()), String.valueOf(tile.getY())});
    
    if (cursor == null) {
      return false;
    }

    try {
      if(cursor.moveToFirst()) {
        
        tile.setLastUsed(cursor.getLong(0));

        // decode image
        byte[] encodedImage = cursor.getBlob(1);
        Bitmap bitmap = BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.length);
        if (bitmap != null) {
          tile.setBitmap(bitmap);
          return true;
        }
        
      }
    } finally {
      cursor.close();
    }

    return false;
  }

  private static final String SQL_EXISTS_TILE = "SELECT 1 FROM " + TileTable.TABLE_NAME + " WHERE " + TileTable.COL_MAP_ID + " = ? AND " + TileTable.COL_LAYER_ID + " = ? AND " + TileTable.COL_X + " = ? AND " + TileTable.COL_Y + "=?";

  public synchronized boolean isTileExisting(Tile tile) {

    Cursor cursor = db.rawQuery(SQL_EXISTS_TILE, new String[] { tile.getMap().getName(), tile.getLayer().getName(), String.valueOf(tile.getX()), String.valueOf(tile.getY())});
    
    if (cursor == null) {
      return false;
    }

    try {
      if(cursor.moveToFirst()) {
        return true;
      }
    } finally {
      cursor.close();
    }

    return false;
  }

  private static final String SQL_UPDATE_LAST_USED = "UPDATE " + TileTable.TABLE_NAME + " SET " + TileTable.COL_LAST_USED + "=? WHERE " + TileTable.COL_MAP_ID + " = ? AND " + TileTable.COL_LAYER_ID + " = ? AND " + TileTable.COL_X + " = ? AND " + TileTable.COL_Y + "=?";

  public synchronized void updateLastUsed(Tile tile) {
    
    SQLiteStatement statement = db.compileStatement(SQL_UPDATE_LAST_USED);
    statement.clearBindings();
    statement.bindLong(1, tile.getLastUsed());
    statement.bindString(2, tile.getMap().getName());
    statement.bindString(3, tile.getLayer().getName());
    statement.bindLong(4, tile.getX());
    statement.bindLong(5, tile.getY());
    statement.executeUpdateDelete();
  }
  
  private static final String SQL_UPDATE_BITMAP = "UPDATE " + TileTable.TABLE_NAME + " SET " + TileTable.COL_LAST_USED + "=?," + TileTable.COL_IMAGE + "=? WHERE " + TileTable.COL_MAP_ID + " = ? AND " + TileTable.COL_LAYER_ID + " = ? AND " + TileTable.COL_X + " = ? AND " + TileTable.COL_Y + "=?";

  public synchronized void updateBitmap(Tile tile, byte[] image) {
    
    SQLiteStatement statement = db.compileStatement(SQL_UPDATE_BITMAP);
    statement.clearBindings();
    statement.bindLong(1, System.currentTimeMillis());
    statement.bindBlob(2, image);
    statement.bindString(3, tile.getMap().getName());
    statement.bindString(4, tile.getLayer().getName());
    statement.bindLong(5, tile.getX());
    statement.bindLong(6, tile.getY());
    int rows = statement.executeUpdateDelete();
    Log.w("TRILLIAN", "updateBitmap() #rows=" + rows);
  }
  
  private static final String SQL_INSERT_TILE = "INSERT INTO " + TileTable.TABLE_NAME + " (" + TileTable.COL_MAP_ID + "," + TileTable.COL_LAYER_ID + "," + TileTable.COL_X + "," + TileTable.COL_Y + "," + TileTable.COL_LAST_USED + "," + TileTable.COL_IMAGE + ") VALUES(?,?,?,?,?,?)";

  public synchronized void insertTile(Tile tile, byte[] image) {
    
    SQLiteStatement statement = db.compileStatement(SQL_INSERT_TILE);
    statement.clearBindings();
    statement.bindString(1, tile.getMap().getName());
    statement.bindString(2, tile.getLayer().getName());
    statement.bindLong(3, tile.getX());
    statement.bindLong(4, tile.getY());
    statement.bindLong(5, System.currentTimeMillis());
    statement.bindBlob(6, image);
    if (statement.executeInsert() >= 0) {
      tileCount++;
      Log.w("TRILLIAN", "Tilecount=" + tileCount);
    }
  }
  
  public synchronized void insertOrReplaceTileBitmap(Tile tile, byte[] image) {
    
    if (isTileExisting(tile)) {
      updateBitmap(tile, image);
      Log.w("TRILLIAN", "Updating bitmap of tile");
    } else {
      insertTile(tile, image);
      Log.w("TRILLIAN", "Inserting new tile");
    }
  }
}
