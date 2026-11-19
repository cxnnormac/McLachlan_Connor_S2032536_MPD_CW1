//
// Name                 Connor McLachlan
// Student ID           S2032536
// Programme of Study   Software Development
//

package org.me.gcu.mclachlan_connor_s2032536;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;

public class MainActivity extends AppCompatActivity implements OnClickListener, OnMapReadyCallback, AdapterView.OnItemClickListener {

    private TextView tvStatusHeader;
    private Button btnRefresh, btnToggleView, btnSearch, btnClear;
    private RadioGroup rgCategory;
    private RadioButton rbAll, rbIncidents, rbRoadworks;
    private EditText etRoadSearch, etDistanceSearch;
    private ViewFlipper viewFlipper;
    private ListView lvSituations;

    private GoogleMap mMap;
    private boolean showingMap = false;

    // Default Glasgow City Centre coordinates if coarse location unavailable
    private double userLat = 55.8668;
    private double userLon = -4.2500;

    private final String clientId = BuildConfig.CLIENT_ID;
    private final String clientKey = BuildConfig.CLIENT_KEY;
    private final String unplannedEventsURL = "https://datex2.trafficscotland.org/rest/2.3/publications/UnplannedEvents/Content.xml";
    private final String roadworksURL = "https://datex2.trafficscotland.org/rest/2.3/publications/CurrentRoadworks/Content.xml";

    private ArrayList<TrafficSituation> masterList = new ArrayList<>();
    private ArrayList<TrafficSituation> displayedList = new ArrayList<>();
    private SituationAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnToggleView = findViewById(R.id.btnToggleView);
        btnSearch = findViewById(R.id.btnSearch);
        btnClear = findViewById(R.id.btnClear);
        rgCategory = findViewById(R.id.rgCategory);
        rbAll = findViewById(R.id.rbAll);
        rbIncidents = findViewById(R.id.rbIncidents);
        rbRoadworks = findViewById(R.id.rbRoadworks);
        etRoadSearch = findViewById(R.id.etRoadSearch);
        etDistanceSearch = findViewById(R.id.etDistanceSearch);
        viewFlipper = findViewById(R.id.viewFlipper);
        lvSituations = findViewById(R.id.lvSituations);

        viewFlipper.setInAnimation(this, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(this, android.R.anim.fade_out);

        btnRefresh.setOnClickListener(this);
        btnToggleView.setOnClickListener(this);
        btnSearch.setOnClickListener(this);
        btnClear.setOnClickListener(this);
        lvSituations.setOnItemClickListener(this);

        rgCategory.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                applyFilters();
            }
        });

        adapter = new SituationAdapter(this, displayedList);
        lvSituations.setAdapter(adapter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fetchCoarseLocation();
        startProgress();
    }

    private void fetchCoarseLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
            return;
        }
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (lm != null) {
                Location lastKnown = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnown != null) {
                    userLat = lastKnown.getLatitude();
                    userLon = lastKnown.getLongitude();
                }
            }
        } catch (Exception e) {
            Log.w("Location", "Using default Glasgow coordinates");
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnRefresh) {
            startProgress();
        } else if (v == btnToggleView) {
            if (!showingMap) {
                viewFlipper.setDisplayedChild(1);
                btnToggleView.setText(R.string.btn_toggle_list);
                showingMap = true;
                updateMapMarkers();
            } else {
                viewFlipper.setDisplayedChild(0);
                btnToggleView.setText(R.string.btn_toggle_map);
                showingMap = false;
            }
        } else if (v == btnSearch) {
            applyFilters();
        } else if (v == btnClear) {
            etRoadSearch.setText("");
            etDistanceSearch.setText("");
            rbAll.setChecked(true);
            applyFilters();
        }
    }

    private void applyFilters() {
        String roadQuery = etRoadSearch.getText().toString().trim().toUpperCase();
        String distQuery = etDistanceSearch.getText().toString().trim();
        double maxKm = -1.0;

        if (!distQuery.isEmpty()) {
            try {
                maxKm = Double.parseDouble(distQuery);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Enter a valid number for distance (km)", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        displayedList.clear();
        for (TrafficSituation s : masterList) {
            if (rbIncidents.isChecked() && s.isRoadwork()) continue;
            if (rbRoadworks.isChecked() && !s.isRoadwork()) continue;

            if (!roadQuery.isEmpty()) {
                boolean matchesRoad = (s.getRoadName() != null && s.getRoadName().contains(roadQuery))
                        || (s.getComment() != null && s.getComment().toUpperCase().contains(roadQuery));
                if (!matchesRoad) continue;
            }

            if (maxKm > 0) {
                if (s.getFromLat() == 0.0 && s.getFromLon() == 0.0) continue;
                float[] results = new float[1];
                Location.distanceBetween(userLat, userLon, s.getFromLat(), s.getFromLon(), results);
                if ((results[0] / 1000.0) > maxKm) continue;
            }

            displayedList.add(s);
        }

        adapter.notifyDataSetChanged();
        tvStatusHeader.setText("Showing " + displayedList.size() + " of " + masterList.size());
        if (showingMap) {
            updateMapMarkers();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final TrafficSituation selected = displayedList.get(position);
        StringBuilder details = new StringBuilder();
        details.append("Category: ").append(selected.isRoadwork() ? "Current Roadworks" : "Unplanned Incident").append("\n");
        details.append("Type: ").append(selected.getSituationType()).append("\n");
        if (selected.getSubType() != null) {
            details.append("Sub-Type: ").append(selected.getSubType()).append("\n");
        }
        details.append("Road: ").append(selected.getRoadName()).append("\n");
        if (selected.getOverallStartTime() != null) {
            details.append("Start Time: ").append(selected.getOverallStartTime()).append("\n");
        }
        if (selected.isRoadwork() && selected.getDelaysType() != null) {
            details.append("Delay Status: ").append(selected.getDelaysType()).append("\n");
        }
        if (selected.getOperationalLanes() != null) {
            details.append("Operational Lanes: ").append(selected.getOperationalLanes()).append("\n");
        }
        if (selected.getLanesRestricted() != null) {
            details.append("Lanes Restricted: ").append(selected.getLanesRestricted()).append("\n");
        }
        details.append("\nDetails:\n").append(selected.getComment());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selected.getRoadName() + " - Situation Details");
        builder.setMessage(details.toString());
        builder.setPositiveButton("View on Map", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                viewFlipper.setDisplayedChild(1);
                btnToggleView.setText(R.string.btn_toggle_list);
                showingMap = true;
                focusSituationOnMap(selected);
            }
        });
        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.4907, -4.2026), 7f));
        updateMapMarkers();
    }

    private void updateMapMarkers() {
        if (mMap == null) return;
        mMap.clear();

        for (TrafficSituation s : displayedList) {
            if (s.getFromLat() != 0.0 && s.getFromLon() != 0.0) {
                LatLng fromPos = new LatLng(s.getFromLat(), s.getFromLon());
                float pinColor = s.isRoadwork()
                        ? ("longDelays".equalsIgnoreCase(s.getDelaysType()) ? BitmapDescriptorFactory.HUE_RED : BitmapDescriptorFactory.HUE_ORANGE)
                        : BitmapDescriptorFactory.HUE_AZURE;

                mMap.addMarker(new MarkerOptions()
                        .position(fromPos)
                        .title(s.getRoadName() + ": " + s.getSituationType())
                        .snippet(s.getComment())
                        .icon(BitmapDescriptorFactory.defaultMarker(pinColor)));

                if (s.getToLat() != 0.0 && s.getToLon() != 0.0 && (s.getFromLat() != s.getToLat() || s.getFromLon() != s.getToLon())) {
                    LatLng toPos = new LatLng(s.getToLat(), s.getToLon());
                    mMap.addPolyline(new PolylineOptions()
                            .add(fromPos, toPos)
                            .width(8f)
                            .color(s.isRoadwork() ? Color.RED : Color.BLUE));
                }
            }
        }
    }

    private void focusSituationOnMap(TrafficSituation s) {
        updateMapMarkers();
        if (mMap != null && s.getFromLat() != 0.0 && s.getFromLon() != 0.0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(s.getFromLat(), s.getFromLon()), 12f));
        } else {
            Toast.makeText(this, "No map coordinates provided for this event.", Toast.LENGTH_SHORT).show();
        }
    }

    public void startProgress() {
        tvStatusHeader.setText("Updating feeds...");
        new Thread(new Task()).start();
    }

    private class Task implements Runnable {
        @Override
        public void run() {
            String unplannedXml = downloadXmlFromUrl(unplannedEventsURL, "CACHE_UNPLANNED");
            String roadworksXml = downloadXmlFromUrl(roadworksURL, "CACHE_ROADWORKS");

            ArrayList<TrafficSituation> parsed = new ArrayList<>();
            if (!unplannedXml.isEmpty()) parsed.addAll(parseData(unplannedXml, false));
            if (!roadworksXml.isEmpty()) parsed.addAll(parseData(roadworksXml, true));

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    masterList.clear();
                    masterList.addAll(parsed);
                    applyFilters();
                    if (masterList.isEmpty()) {
                        Toast.makeText(MainActivity.this, "No data loaded. Check network or credentials.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        private String downloadXmlFromUrl(String urlString, String cacheKey) {
            StringBuilder resultBuilder = new StringBuilder();
            SharedPreferences prefs = getSharedPreferences("TrafficScotlandCache", Context.MODE_PRIVATE);

            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(10000);
                urlConnection.setReadTimeout(15000);

                String encodedAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientKey).getBytes());
                urlConnection.setRequestProperty("Authorization", encodedAuth);

                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        resultBuilder.append(inputLine);
                    }
                    in.close();
                } finally {
                    urlConnection.disconnect();
                }
            } catch (IOException e) {
                Log.e("MyTask", "Network error for " + cacheKey + ": " + e.getMessage());
            }

            String xmlResult = resultBuilder.toString();
            int i = xmlResult.indexOf("<?");
            if (i >= 0) {
                xmlResult = xmlResult.substring(i);
                prefs.edit().putString(cacheKey, xmlResult).apply();
            } else {
                xmlResult = prefs.getString(cacheKey, "");
            }
            return xmlResult;
        }

        private ArrayList<TrafficSituation> parseData(String dataToParse, boolean isRoadworkFeed) {
            ArrayList<TrafficSituation> list = new ArrayList<>();
            TrafficSituation currentSituation = null;

            boolean insideASituation = false;
            boolean insideComment = false;
            String tempLat = null;
            String tempLon = null;

            try {
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                xpp.setInput(new StringReader(dataToParse));

                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String tagName = xpp.getName();

                        if (tagName.equalsIgnoreCase("situation")) {
                            insideASituation = true;
                            currentSituation = new TrafficSituation();
                            currentSituation.setRoadwork(isRoadworkFeed);
                            if (xpp.getAttributeCount() > 0) {
                                currentSituation.setSituationId(xpp.getAttributeValue(0));
                            }
                        } else if (insideASituation && currentSituation != null) {
                            if (tagName.equalsIgnoreCase("situationRecord")) {
                                if (xpp.getAttributeCount() > 0) {
                                    currentSituation.setSituationType(xpp.getAttributeValue(0));
                                }
                            } else if (tagName.equalsIgnoreCase("generalPublicComment")) {
                                insideComment = true;
                            } else if (insideComment && tagName.equalsIgnoreCase("value")) {
                                currentSituation.setComment(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("overallStartTime")) {
                                currentSituation.setOverallStartTime(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("delaysType")) {
                                currentSituation.setDelaysType(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("numberOfOperationalLanes")) {
                                currentSituation.setOperationalLanes(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("numberOfLanesRestricted")) {
                                currentSituation.setLanesRestricted(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("roadOrCarriagewayOrLaneManagementType")
                                    || tagName.equalsIgnoreCase("abnormalTrafficType")
                                    || tagName.equalsIgnoreCase("roadMaintenanceType")) {
                                currentSituation.setSubType(xpp.nextText());
                            } else if (tagName.equalsIgnoreCase("latitude")) {
                                tempLat = xpp.nextText();
                            } else if (tagName.equalsIgnoreCase("longitude")) {
                                tempLon = xpp.nextText();
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        String tagName = xpp.getName();

                        if (tagName.equalsIgnoreCase("situation")) {
                            if (currentSituation != null) {
                                list.add(currentSituation);
                            }
                            insideASituation = false;
                            currentSituation = null;
                        } else if (tagName.equalsIgnoreCase("generalPublicComment")) {
                            insideComment = false;
                        } else if (tagName.equalsIgnoreCase("from") || tagName.equalsIgnoreCase("pointCoordinates")) {
                            if (currentSituation != null && tempLat != null && tempLon != null) {
                                try {
                                    double lat = Double.parseDouble(tempLat);
                                    double lon = Double.parseDouble(tempLon);
                                    if (currentSituation.getFromLat() == 0.0 && currentSituation.getFromLon() == 0.0) {
                                        currentSituation.setFromLat(lat);
                                        currentSituation.setFromLon(lon);
                                    } else if (currentSituation.getToLat() == 0.0 && currentSituation.getToLon() == 0.0) {
                                        currentSituation.setToLat(lat);
                                        currentSituation.setToLon(lon);
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                            tempLat = null;
                            tempLon = null;
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (XmlPullParserException | IOException e) {
                Log.e("Parsing", "Error parsing XML: " + e.toString());
            }

            return list;
        }
    }
}