package ch.trillian.dufour;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TileTable {

  public static final String TABLE_NAME = "TILE";
  public static final String COL_MAP_ID = "MAP_ID";
  public static final String COL_LAYER_ID = "LAYER_ID";
  public static final String COL_X = "X";
  public static final String COL_Y = "Y";
  public static final String COL_IMAGE = "IMAGE";

  private static final String DATABASE_CREATE = "CREATE TABLE " 
      + TABLE_NAME
      + "(" 
      + COL_MAP_ID + " TEXT NOT NULL, " 
      + COL_LAYER_ID + " TEXT NOT NULL, " 
      + COL_X + " INTEGER NOT NULL, " 
      + COL_Y + " INTEGER NOT NULL, " 
      + COL_IMAGE + " BLOB NOT NULL,"
      + "PRIMARY KEY (" + COL_MAP_ID + ", " + COL_LAYER_ID + ", " + COL_X + ", " + COL_Y + ")"
      + ");";

  public static void onCreate(SQLiteDatabase database) {
    
    database.execSQL(DATABASE_CREATE);
  }

  public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
    
    Log.w(TileTable.class.getName(), "Upgrading table " + TABLE_NAME + " from version " + oldVersion + " to " + newVersion + ", which will destroy all old data.");
    database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    onCreate(database);
  }
}