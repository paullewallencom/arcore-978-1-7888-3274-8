package com.olucurious.artreasurehunt;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private GoogleMap mMap;
    private Context context;

    public static Intent launchActivity(Context context) {
        return new Intent(context, MapsActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        context = this;
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        // Add a marker in Sydney and move the camera

        Drawable circleDrawable = getResources().getDrawable(R.drawable.ic_andy_robot, context.getTheme());

        BitmapDescriptor icon = getMarkerIconFromDrawable(circleDrawable);

        LatLng camera;

        FirebaseManager firebaseManager = new FirebaseManager(context);
        firebaseManager.getTreasureSpots(new FirebaseManager.GetTreasureSpotsListener() {
            @Override
            public void onDataReady(List<HashMap<String, Object>> hashMapList) {
                LatLng location = new LatLng(0, 0);
                for (HashMap<String, Object> keyMap : hashMapList){
                    Log.e("ANCHOR", "anchor_id:: "+keyMap.get("anchor_id"));
                    location = new LatLng(Double.parseDouble(keyMap.get("latitude").toString()), Double.parseDouble(keyMap.get("longitude").toString()));
                    Marker marker = mMap.addMarker(new MarkerOptions().position(location).icon(icon));
                    marker.setTag(keyMap.get("anchor_id"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
            }

            @Override
            public void onError() {
                Toast.makeText(context, "Error fetching treasure spots, please try again", Toast.LENGTH_LONG).show();
            }
        });


    }

    private BitmapDescriptor getMarkerIconFromDrawable(Drawable drawable) {
        Canvas canvas = new Canvas();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        Log.e("onMarkerClick", "onMarkerClick");

        if (marker.getTag() != null){
            Log.e("onMarkerClick", marker.getTag().toString());
            context.startActivity(MainActivity.launchActivity(context, marker.getTag().toString()));
        }
        return true;
    }
}
