package ch.trillian.dufour;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class SwissGeocoderProvider extends ContentProvider {

  private static final String BASE_URL = "http://map.geo.admin.ch/main/1392444380/rest/services/ech/SearchServer?type=locations&features=&lang=de";
  private static final String LOCATION_PARAM = "searchText";
  
  private static final String TAG = "GEOCODER";
  
  private static final int DEFAULT_LIMIT = 10;

  
  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {

    return 0;
  }

  @Override
  public String getType(Uri uri) {

    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {

    return null;
  }

  @Override
  public boolean onCreate() {

    return false;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    
    // get query string
    String query = uri.getLastPathSegment();
    
    // get limit
    String limitStr = uri.getQueryParameter("limit");
    int limit = DEFAULT_LIMIT;
    if (limitStr != null) {
      try {
        limit = Integer.valueOf(limitStr);
      } catch (NumberFormatException e) {
        Log.w(TAG, "query() limit parameter not a number in URI: " + uri.toString());
      }
    }
    Log.i(TAG, "query() limit=" + limit);
    
    MatrixCursor cursor = new MatrixCursor(new String[] {"_ID", SearchManager.SUGGEST_COLUMN_TEXT_1, SearchManager.SUGGEST_COLUMN_TEXT_2, SearchManager.SUGGEST_COLUMN_INTENT_DATA });
    
    try {
      
      // get list of matching addresses
      String response = requestAddresses(query);
      parseResponse(response, cursor);
      
//      List<Address> addressList = geocoder.getFromLocationName(query, limit, lowerLeft[1], lowerLeft[0], upperRight[1], upperRight[0]);
//      
//      int i=0;
//      for (Address address : addressList) {
//        
//        Log.i(TAG, "query() address: " + address);
//
//        if (address.hasLongitude() && address.hasLatitude()) {
//          
//          // build first line
//          StringBuilder text1 = new StringBuilder();
//          if (address.getPostalCode() != null) {
//            text1.append(address.getPostalCode());
//            text1.append(" ");
//          }
//          if (address.getLocality() != null) {
//            text1.append(address.getLocality());
//          }
//          
//          // build second line
//          StringBuilder text2 = new StringBuilder();
//          if (address.getAddressLine(0) != null) {
//            text2.append(address.getAddressLine(0));
//          }
//          
//          // build intent's data
//          Uri.Builder uriBuilder = Uri.parse("content://ch.trillian.dufour.geocoder/").buildUpon();
//          uriBuilder.appendQueryParameter("longitude", String.valueOf(address.getLongitude()));
//          uriBuilder.appendQueryParameter("latitude", String.valueOf(address.getLatitude()));
//
//          // add to result
//          cursor.addRow(new Object[] { i++, text1.toString(), text2.toString(), uriBuilder.toString() });
//        }
//      }
    } catch (Exception e) {
      Log.i(TAG, "query() getFromLocationName failed: " + e.getMessage());
    }
    
    return cursor;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

    return 0;
  }
  
  private void parseResponse(String response, MatrixCursor cursor) {
    
    try {
      JSONObject result = new JSONObject(response);
      JSONArray locations = result.getJSONArray("results");

      // loop over all locations in response
      for (int i = 0; i < locations.length(); i++) {
        try {
          
          // parse location
          JSONObject location = locations.getJSONObject(i);
          JSONObject attributes = location.getJSONObject("attrs");
          String origin = attributes.getString("origin");
          String box = attributes.getString("geom_st_box2d");
          String label = attributes.getString("label");
          Log.i(TAG, "parseResponse() origin=" + origin + " box=" + box + " label=" + label);
          
          // parse box
          box = box.replace("BOX(", "");
          box = box.replace(")", "");
          box = box.replace(",", " ");
          String[] coordinates = box.split(" ");

          if ("gg25".equals(origin)) {
            continue;
          }
          
          try {
            double x = (Double.valueOf(coordinates[0]) + Double.valueOf(coordinates[2])) / 2d;
            double y = (Double.valueOf(coordinates[1]) + Double.valueOf(coordinates[3])) / 2d;
            double[] wgs84 = Ch1903.ch1903toWgs84to(x, y, 0);
            Log.i(TAG, "x=" + x + "y=" + y);

            // build intent's data
            Uri.Builder uriBuilder = Uri.parse("content://ch.trillian.dufour.geocoder/").buildUpon();
            uriBuilder.appendQueryParameter("longitude", String.valueOf(wgs84[0]));
            uriBuilder.appendQueryParameter("latitude", String.valueOf(wgs84[1]));
            
            // prepare label
            Log.i(TAG, "LABEL=" + label);
            label = label.replaceAll("^<b>", "");
            label = label.replaceAll("</b>", "");
            Log.i(TAG, "LABEL=" + label);
            String[] lines = label.split("<b>", 2);
            
            // build result row
            cursor.addRow(new Object[] { i++, lines[0] != null ? lines[0] : "", lines.length > 1 && lines[1] != null ? lines[1] : "", uriBuilder.toString() });
            
          } catch (NumberFormatException e) {
            continue;
          }
          
  
        } catch (JSONException e) {
          Log.i(TAG, "parse location failed: " + e.getMessage());
        }
      }
      
    } catch (JSONException e) {
      Log.i(TAG, "parse response failed: " + e.getMessage());
    }
  }
  
  private String requestAddresses(String location) {
    
    try {
  
      // build URL
      Uri.Builder uriBuilder = Uri.parse(BASE_URL).buildUpon();
      uriBuilder.appendQueryParameter(LOCATION_PARAM, location);
      
      
      // open http stream
      URL url = new URL(uriBuilder.toString());
      
      Log.i(TAG, "Request URL: " + url.toString());

      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("referer", "http://map.geo.admin.ch/");
      
      // read response
      InputStream inputStream = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      StringBuilder builder = new StringBuilder ();
      while ((line = reader.readLine()) != null) {
          builder.append(line);
      }
      
      inputStream.close();
      connection.disconnect();
      
      Log.i(TAG, "Response: " + builder.toString());

      return builder.toString();
  
    } catch (Exception e) {
      Log.w(TAG, "Exception: " + e.getMessage(), e);
    }
    
    return null;
  }
}
