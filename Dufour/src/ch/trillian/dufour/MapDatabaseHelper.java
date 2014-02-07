package ch.trillian.dufour;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class MapDatabaseHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "map.db";
  private static final int DATABASE_VERSION = 1;

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
  
  private static final String SQL_GET_TILE_COUNT = "SELECT COUNT(*) FROM " + TileTable.TABLE_NAME;

  public int getTileCount() {
    
    SQLiteDatabase db = getReadableDatabase();
    
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
      db.close();
    }

    return -1;
  }
  
  private static final String SQL_GET_TILE_IMAGE = "SELECT " + TileTable.COL_IMAGE + " FROM " + TileTable.TABLE_NAME + " WHERE " + TileTable.COL_MAP_ID + " = ? AND " + TileTable.COL_LAYER_ID + " = ? AND " + TileTable.COL_X + " = ? AND " + TileTable.COL_Y + "=?";

  public synchronized byte[] getTileImage(Tile tile) {

    SQLiteDatabase db = getReadableDatabase();
    
    Cursor cursor = db.rawQuery(SQL_GET_TILE_IMAGE, new String[] { tile.getMap().getName(), tile.getLayer().getName(), String.valueOf(tile.getX()), String.valueOf(tile.getY())});
    
    if (cursor == null) {
      return null;
    }

    try {
      if(cursor.moveToFirst()) {
        return cursor.getBlob(0);
      }
    } finally {
      cursor.close();
      db.close();
    }

    return null;
  }

  private static final String SQL_INSERT = "INSERT OR REPLACE INTO " + TileTable.TABLE_NAME + " (" + TileTable.COL_MAP_ID + "," + TileTable.COL_LAYER_ID + "," + TileTable.COL_X + "," + TileTable.COL_Y + "," + TileTable.COL_IMAGE + ") VALUES(?,?,?,?,?)";

  public synchronized void insertOrReplaceTileBitmap(Tile tile, byte[] image) {
    
    SQLiteDatabase db = getWritableDatabase();

    SQLiteStatement statement = db.compileStatement(SQL_INSERT);
    statement.clearBindings();
    statement.bindString(1, tile.getMap().getName());
    statement.bindString(2, tile.getLayer().getName());
    statement.bindLong(3, tile.getX());
    statement.bindLong(4, tile.getY());
    statement.bindBlob(5, image);
    statement.executeInsert();
    db.close();
  }
}
