package com.olucurious.artreasurehunt;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

/**
 * Node that represents a treasure item.
 *
 * <p>The treasure creates two child nodes when it is activated:
 *
 * <ul>
 *   <li>The treasure Andy object itself.
 *   <li>An info card, renders an Android View that displays the name of the treasure and a button to collect it. This can be
 *       toggled on and off.
 * </ul>
 *
 */
public class TreasureNode extends Node implements Node.OnTapListener {
    private final String treasureName;
    private final String cloudAnchorId;
    private final ModelRenderable treasureRenderable;

    private Node infoCard;
    private Node treasureVisual;
    private final Context context;

    private static final float INFO_CARD_Y_POS_COEFF = 0.55f;

    private FirebaseManager firebaseManager;

    public TreasureNode(
            Context context,
            String treasureName,
            String cloudAnchorId,
            ModelRenderable treasureRenderable) {
        this.context = context;
        this.treasureName = treasureName;
        this.cloudAnchorId = cloudAnchorId;
        this.treasureRenderable = treasureRenderable;
        this.firebaseManager = new FirebaseManager(context);
        setOnTapListener(this);
    }

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void onActivate() {

        if (getScene() == null) {
            throw new IllegalStateException("Scene is null!");
        }

        if (infoCard == null) {
            infoCard = new Node();
            infoCard.setParent(this);
            infoCard.setEnabled(false);
            infoCard.setLocalPosition(new Vector3(0.0f, .3f * INFO_CARD_Y_POS_COEFF, 0.0f));

            ViewRenderable.builder()
                    .setView(context, R.layout.treasure_card)
                    .build()
                    .thenAccept(
                            (renderable) -> {
                                infoCard.setRenderable(renderable);
                                renderable.getView().findViewById(R.id.collect_item_btn).setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        firebaseManager.collectNewTreasure(cloudAnchorId);
                                        Toast.makeText(context, "Treasure collected successfully", Toast.LENGTH_LONG).show();
                                        android.os.Handler handler = new android.os.Handler();
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                context.startActivity(MainActivity.launchActivity(context, ""));
                                            }
                                        }, 2000);
                                    }
                                });
                            })
                    .exceptionally(
                            (throwable) -> {
                                throw new AssertionError("Could not load treasure info card view.", throwable);
                            });
        }

        if (treasureVisual == null) {
            treasureVisual = new Node();
            treasureVisual.setParent(this);
            treasureVisual.setRenderable(treasureRenderable);
            treasureVisual.setLocalScale(new Vector3(1.0f, 1.0f, 1.0f));
        }
    }

    @Override
    public void onTap(HitTestResult hitTestResult, MotionEvent motionEvent) {
        if (infoCard == null) {
            return;
        }

        infoCard.setEnabled(!infoCard.isEnabled());
    }
}