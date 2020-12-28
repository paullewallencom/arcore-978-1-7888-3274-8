/*
 * Copyright 2018 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.olucurious.artreasurehunt;

import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A helper class to manage all communications with Firebase.
 */
class FirebaseManager {
    private static final String TAG =
            MainActivity.class.getSimpleName() + "." + FirebaseManager.class.getSimpleName();


    // Names of the nodes used in the Firebase Database
    private static final String ROOT_FIREBASE_TREASURE = "treasure_spots";
    private static final String ROOT_TREASURE_HUNTERS = "treasure_hunters";

    // Some common keys and values used when writing to the Firebase Database.
    private static final String KEY_DISPLAY_NAME = "display_name";
    private static final String KEY_ANCHOR_ID = "anchor_id";
    private static final String KEY_TIMESTAMP = "updated_at_timestamp";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_USER_ID = "treasure_hunter_id";
    private static final String KEY_COLLECTED_TREASURES = "collected_treasures";
    private static final String KEY_COLLECTED_TREASURES_COUNT = "collected_treasures_count";

    private final FirebaseApp app;
    private final DatabaseReference treasureSpotListRef;
    private final DatabaseReference treasureHuntersRef;

    private Context context;

    /**
     * Default constructor for the FirebaseManager.
     *
     * @param context The application context.
     */
    FirebaseManager(Context context) {
        app = FirebaseApp.initializeApp(context);
        this.context = context;
        if (app != null) {
            DatabaseReference rootRef = FirebaseDatabase.getInstance(app).getReference();
            treasureSpotListRef = rootRef.child(ROOT_FIREBASE_TREASURE);
            treasureHuntersRef = rootRef.child(ROOT_TREASURE_HUNTERS);

            DatabaseReference.goOnline();
        } else {
            Log.d(TAG, "Could not connect to Firebase Database!");
            treasureSpotListRef = null;
            treasureHuntersRef = null;
        }
    }


    // Add a new user to treasure_hunters
    void addNewTreasureHunter(String name){
        DatabaseReference usersRef = treasureHuntersRef.push();
        String userId = usersRef.getKey();

        usersRef.child(KEY_DISPLAY_NAME).setValue(name);
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("userId", userId).apply();
    }

    /**
     * Stores the given anchor ID as a treasure spot.
     */
    void storeAnchorIdTreasureSpot(String cloudAnchorId, Location location) {
        DatabaseReference treasureSpotRef = treasureSpotListRef.child(cloudAnchorId);
        treasureSpotRef.child(KEY_LONGITUDE).setValue(location.getLongitude());
        treasureSpotRef.child(KEY_LATITUDE).setValue(location.getLatitude());
        treasureSpotRef.child(KEY_ANCHOR_ID).setValue(cloudAnchorId);
        treasureSpotRef.child(KEY_TIMESTAMP).setValue(System.currentTimeMillis());
        treasureSpotRef.child(KEY_USER_ID).setValue(PreferenceManager.getDefaultSharedPreferences(context).getString("userId", ""));
    }

    /**
     * Collect new treasure item.
     */
    void collectNewTreasure(String cloudAnchorId) {
        getUserCollectedTreasures(new CollectedTreasuresListener() {
            @Override
            public void onDataReady(List<String> stringList) {
                stringList.add(cloudAnchorId);

                String userId = PreferenceManager.getDefaultSharedPreferences(context).getString("userId", "");
                DatabaseReference treasureHunterRef = treasureHuntersRef.child(userId);
                treasureHunterRef.child(KEY_COLLECTED_TREASURES).setValue(stringList);
                treasureHunterRef.child(KEY_COLLECTED_TREASURES_COUNT).setValue(stringList.size());
            }

            @Override
            public void onError() {
                List<String> collectedTreasures = new ArrayList<>();
                collectedTreasures.add(cloudAnchorId);

                String userId = PreferenceManager.getDefaultSharedPreferences(context).getString("userId", "");
                DatabaseReference treasureHunterRef = treasureHuntersRef.child(userId);
                treasureHunterRef.child(KEY_COLLECTED_TREASURES).setValue(collectedTreasures);
                treasureHunterRef.child(KEY_COLLECTED_TREASURES_COUNT).setValue(collectedTreasures.size());
            }
        });
    }


    /**
     * Listener for list of collected treasures by a user
     */
    interface CollectedTreasuresListener {
        void onDataReady(List<String> stringList);

        void onError();
    }

    @SuppressWarnings("unchecked")
    void getUserCollectedTreasures(CollectedTreasuresListener collectedTreasuresListener){
        String userId = PreferenceManager.getDefaultSharedPreferences(context).getString("userId", "");
        treasureHuntersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() != null){
                    HashMap<String, Object> userMap = (HashMap<String, Object>) dataSnapshot.getValue();
                    Log.e("userMap", userMap.toString());
                    List<String> collectedTreasures = new ArrayList<>();
                    if (userMap.containsKey(KEY_COLLECTED_TREASURES)){
                        collectedTreasures = (List<String>)userMap.get(KEY_COLLECTED_TREASURES);
                    }
                    collectedTreasuresListener.onDataReady(collectedTreasures);
                } else {
                    collectedTreasuresListener.onError();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                collectedTreasuresListener.onError();
            }
        });
    }


    /**
     * Listener for list of treasure spots
     */
    interface GetTreasureSpotsListener {
        void onDataReady(List<HashMap<String, Object>> hashMapList);

        void onError();
    }

    @SuppressWarnings("unchecked")
    void getTreasureSpots(GetTreasureSpotsListener getTreasureSpotsListener){
        getUserCollectedTreasures(new CollectedTreasuresListener() {
            @Override
            public void onDataReady(List<String> stringList) {
                treasureSpotListRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() != null){
                            HashMap<String, Object> treasureSpotsMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            Log.e("treasureSpotsMap", treasureSpotsMap.toString());
                            List<HashMap<String, Object>> newList = new ArrayList<>();
                            for (String key : treasureSpotsMap.keySet()) {
                                HashMap<String, Object> treasureSpMap = (HashMap<String, Object>) treasureSpotsMap.get(key);
                                if (!stringList.contains(treasureSpMap.get(KEY_ANCHOR_ID).toString()))
                                    newList.add(treasureSpMap);
                            }
                            getTreasureSpotsListener.onDataReady(newList);
                        } else {
                            getTreasureSpotsListener.onError();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        getTreasureSpotsListener.onError();
                    }
                });
            }

            @Override
            public void onError() {
                treasureSpotListRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        if (dataSnapshot.getValue() != null){
                            HashMap<String, Object> treasureSpotsMap = (HashMap<String, Object>) dataSnapshot.getValue();
                            Log.e("treasureSpotsMap", treasureSpotsMap.toString());
                            List<HashMap<String, Object>> newList = new ArrayList<>();
                            for (String key : treasureSpotsMap.keySet()) {
                                HashMap<String, Object> treasureSpMap = (HashMap<String, Object>) treasureSpotsMap.get(key);
                                newList.add(treasureSpMap);
                            }
                            getTreasureSpotsListener.onDataReady(newList);
                        } else {
                            getTreasureSpotsListener.onError();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        getTreasureSpotsListener.onError();
                    }
                });
            }
        });
    }


    /**
     * Listener for list of users.
     */
    interface GetUsersListener {
        void onDataReady(List<HashMap<String, Object>> hashMapList);

        void onError();
    }

    @SuppressWarnings("unchecked")
    void getUsersLeaderboard(GetUsersListener getUsersListener){
        treasureHuntersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.getValue() != null){
                    HashMap<String, Object> treasureSpotsMap = (HashMap<String, Object>) dataSnapshot.getValue();
                    Log.e("treasureSpotsMap", treasureSpotsMap.toString());
                    List<HashMap<String, Object>> newList = new ArrayList<>();
                    for (String key : treasureSpotsMap.keySet()) {
                        HashMap<String, Object> treasureSpMap = (HashMap<String, Object>) treasureSpotsMap.get(key);
                        newList.add(treasureSpMap);
                    }
                    getUsersListener.onDataReady(newList);
                } else {
                    getUsersListener.onError();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                getUsersListener.onError();
            }
        });
    }
}
