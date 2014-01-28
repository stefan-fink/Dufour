package ch.trillian.dufour;

import ch.trillian.dufour.R;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class MapActivity extends Activity {

  private final Map ch1903Map = createMap("ch1903");
  private MapView mapView;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_map);
    mapView = (MapView) findViewById(R.id.map_view);
    mapView.setMap(ch1903Map, 1);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {

    getMenuInflater().inflate(R.menu.map, menu);
    return true;
  }
  
  private Map createMap(String mapName) {
    
    Layer[] layers = {
        new Layer("CH1903-18", 256, 256, 20, 20),
        new Layer("CH1903-19", 256, 256, 20, 20)
    };
    
    return new Map(mapName, layers);
  }
}
