package com.olucurious.artreasurehunt;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CloudAnchorFragment fragment;
    private Anchor cloudAnchor;

    private enum AppAnchorState {
        NONE,
        HOSTING,
        HOSTED,
        RESOLVING,
        RESOLVED
    }

    private AppAnchorState appAnchorState = AppAnchorState.NONE;

    private SnackbarHelper snackbarHelper = new SnackbarHelper();

    private Context context;

    private FirebaseManager firebaseManager;

    private GPSTracker gpsTracker;

    private static final String EXTRA_CLOUD_ANCHOR_ID = "EXTRA_CLOUD_ANCHOR_ID";

    public static Intent launchActivity(Context context, String cloudAnchorId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra(EXTRA_CLOUD_ANCHOR_ID, cloudAnchorId);
        return intent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        fragment = (CloudAnchorFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        fragment.getPlaneDiscoveryController().hide();
        fragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        firebaseManager = new FirebaseManager(context);

        if (getIntent().hasExtra(EXTRA_CLOUD_ANCHOR_ID)){
            String cloudAnchorId = getIntent().getStringExtra(EXTRA_CLOUD_ANCHOR_ID);
            if (!cloudAnchorId.equals("")){
                findViewById(R.id.cardView).setVisibility(View.GONE);
                Handler handler = new Handler();
                handler.postDelayed(() -> resolveAnchor(cloudAnchorId), 5000);
            }
        }

        if (PreferenceManager.getDefaultSharedPreferences(context).getString("userId", "").equals("")){
            TextInputDialogFragment dialog = new TextInputDialogFragment();
            dialog.setOkListener(MainActivity.this::setUserName);
            dialog.show(getSupportFragmentManager(), "Resolve");
        }


        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                18);
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                13);

        gpsTracker = new GPSTracker(context);
        // Check if GPS enabled
        if (!(gpsTracker.canGetLocation() && gpsTracker.getLatitude() > 0 && gpsTracker.getLongitude() > 0)) {
            snackbarHelper.showMessage(this, "Error getting your location, please turn on GPS and reopen the app...");
            return;
        }


        Button clearButton = findViewById(R.id.clear_button);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setCloudAnchor(null);
            }
        });

        Button locateButton = findViewById(R.id.locate_button);
        locateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                context.startActivity(MapsActivity.launchActivity(context));
            }
        });


        Button leaderboardButton = findViewById(R.id.leaderboard_button);
        leaderboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.menu).setVisibility(View.GONE);
                findViewById(R.id.leaderboard_list).setVisibility(View.VISIBLE);
                firebaseManager.getUsersLeaderboard(new FirebaseManager.GetUsersListener() {
                    @Override
                    public void onDataReady(List<HashMap<String, Object>> hashMapList) {
                        LinearLayout linearLayout = findViewById(R.id.leaderboard_list);
                        for(HashMap<String, Object> objectHashMap : hashMapList){
                            linearLayout.addView(getLeaderboardEntryView(objectHashMap.get("display_name").toString(), Integer.parseInt(objectHashMap.get("collected_treasures_count").toString())));
                        }
                    }

                    @Override
                    public void onError() {

                    }
                });
            }
        });

        Button dropTreasureButton = findViewById(R.id.drop_button);
        dropTreasureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearButton.setVisibility(View.VISIBLE);
                findViewById(R.id.cardView).setVisibility(View.GONE);

                fragment.setOnTapArPlaneListener(
                        (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {

                            if (plane.getType() != Plane.Type.HORIZONTAL_UPWARD_FACING ||
                                    appAnchorState != AppAnchorState.NONE){
                                return;
                            }

                            Anchor newAnchor = fragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());

                            setCloudAnchor(newAnchor);

                            appAnchorState = AppAnchorState.HOSTING;
                            snackbarHelper.showMessage((Activity) context, "Now hosting anchor...");


                            buildObject(fragment, cloudAnchor, Uri.parse("andy.sfb"));

                        }
                );
            }
        });
    }

    View getLeaderboardEntryView(String user_name, int score) {
        View rootView = getLayoutInflater().inflate(R.layout.leaderboard_rank_item, null);
        TextView userNameTextView = (TextView)rootView.findViewById(R.id.user_name);
        userNameTextView.setText(user_name);
        TextView userScoreTextView = (TextView)rootView.findViewById(R.id.user_score);
        userScoreTextView.setText(String.valueOf(score));
        return rootView;
    }

    private void setUserName(String name){
        firebaseManager.addNewTreasureHunter(name);
    }

    private void resolveAnchor(String cloudAnchorId){
        Anchor resolvedAnchor = fragment.getArSceneView().getSession().resolveCloudAnchor(cloudAnchorId);
        setCloudAnchor(resolvedAnchor);
        resolveAndbuildObject(fragment, cloudAnchor, Uri.parse("andy.sfb"), cloudAnchorId);
        snackbarHelper.showMessage(this, "Now Resolving Anchor...");
        appAnchorState = AppAnchorState.RESOLVING;
    }

    private void setCloudAnchor (Anchor newAnchor){
        if (cloudAnchor != null){
            cloudAnchor.detach();
        }

        cloudAnchor = newAnchor;
        appAnchorState = AppAnchorState.NONE;
        snackbarHelper.hide(this);
    }

    private void onUpdateFrame(FrameTime frameTime){
        checkUpdatedAnchor();
    }

    private synchronized void checkUpdatedAnchor(){
        if (appAnchorState != AppAnchorState.HOSTING && appAnchorState != AppAnchorState.RESOLVING){
            return;
        }
        Anchor.CloudAnchorState cloudState = cloudAnchor.getCloudAnchorState();
        if (appAnchorState == AppAnchorState.HOSTING) {
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error hosting anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS) {
                firebaseManager.storeAnchorIdTreasureSpot(cloudAnchor.getCloudAnchorId(), gpsTracker.getLocation());
                snackbarHelper.showMessageWithDismiss(this, "Treasure spot saved successfully!");

                appAnchorState = AppAnchorState.HOSTED;
            }
        }

        else if (appAnchorState == AppAnchorState.RESOLVING){
            if (cloudState.isError()) {
                snackbarHelper.showMessageWithDismiss(this, "Error resolving anchor.. "
                        + cloudState);
                appAnchorState = AppAnchorState.NONE;
            } else if (cloudState == Anchor.CloudAnchorState.SUCCESS){
                snackbarHelper.showMessageWithDismiss(this, "Anchor resolved successfully");
                appAnchorState = AppAnchorState.RESOLVED;
            }
        }

    }


    private void buildObject(ArFragment fragment, Anchor anchor, Uri model) {
        ModelRenderable.builder()
                .setSource(fragment.getContext(), model)
                .build()
                .thenAccept(renderable -> addNewTreasureNodeToScene(fragment, anchor, renderable))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));
    }


    private void resolveAndbuildObject(ArFragment fragment, Anchor anchor, Uri model, String cloudAnchorId) {
        ModelRenderable.builder()
                .setSource(fragment.getContext(), model)
                .build()
                .thenAccept(renderable -> addResolvedTreasureNodeToScene(fragment, anchor, renderable, cloudAnchorId))
                .exceptionally((throwable -> {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(throwable.getMessage())
                            .setTitle("Error!");
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return null;
                }));

    }

    private void addResolvedTreasureNodeToScene(ArFragment fragment, Anchor anchor, ModelRenderable renderable, String cloudAnchorId) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TreasureNode node = new TreasureNode(fragment.getContext(), "Andy", cloudAnchorId, renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
    }

    private void addNewTreasureNodeToScene(ArFragment fragment, Anchor anchor, Renderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(fragment.getTransformationSystem());
        node.setRenderable(renderable);
        node.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }
}

