package ch.trillian.dufour;

import java.util.List;
import java.util.Locale;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;

public class GeocoderProvider extends ContentProvider {

  private static final String TAG = "GEOCODE";
  
  private double[] lowerLeft = Ch1903.ch1903toWgs84to(420000, 20000, 0);
  private double[] upperRight = Ch1903.ch1903toWgs84to(850000, 350000, 0);
  
  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getType(Uri uri) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean onCreate() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    
    Log.i(TAG, "query() uri=" + uri);

    String query = uri.getLastPathSegment();
    
    Log.i(TAG, "query() query=" + query);
    
    Geocoder geocoder = new Geocoder(getContext(), new Locale("de", "CH"));
    MatrixCursor cursor = new MatrixCursor(new String[] {"_ID", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA });
    try {
      // Log.w(TAG, String.format("BOUNDS: %f %f %f %f", lowerLeft[1], lowerLeft[0], upperRight[1], upperRight[0]));
      List<Address> addressList = geocoder.getFromLocationName(query, 10, lowerLeft[1], lowerLeft[0], upperRight[1], upperRight[0]);
      // List<Address> addressList = geocoder.getFromLocationName("*" + query + "*", 100);
      int i=0;
      for (Address address : addressList) {
        
        Log.i("TRILLIAN", "query() address: " + address);

        if (address.hasLongitude() && address.hasLatitude()) {
          StringBuilder text1 = new StringBuilder();
          if (address.getPostalCode() != null) {
            text1.append(address.getPostalCode());
            text1.append(" ");
          }
          if (address.getLocality() != null) {
            text1.append(address.getLocality());
          }
          
          StringBuilder text2 = new StringBuilder();
          if (address.getAddressLine(0) != null) {
            text2.append(address.getAddressLine(0));
          }
          
          Uri.Builder uriBuilder = Uri.parse("content://ch.trillian.dufour.geocoder/").buildUpon();
          uriBuilder.appendQueryParameter("longitude", String.valueOf(address.getLongitude()));
          uriBuilder.appendQueryParameter("latitude", String.valueOf(address.getLatitude()));
          
          Log.i("TRILLIAN", "query() address=: " + uriBuilder.toString());
          
          cursor.addRow(new Object[] { i++, text1.toString(), text2.toString(), uriBuilder.toString() });
        }
      }
    } catch (Exception e) {
      Log.i("TRILLIAN", "query() getFromLocationName failed: " + e.getMessage());
    }
    
    return cursor;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
    // TODO Auto-generated method stub
    return 0;
  }

}
