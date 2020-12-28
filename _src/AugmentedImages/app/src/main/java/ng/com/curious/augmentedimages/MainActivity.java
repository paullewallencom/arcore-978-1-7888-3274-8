package ng.com.curious.augmentedimages;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private ImageView fitToScanView;

    // Augmented image and its associated center pose anchor
    private final Map<AugmentedImage, AugmentedWebViewNode> augmentedImageMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()){
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }


    private void onUpdateFrame(FrameTime frameTime){
        Frame frame = arFragment.getArSceneView().getArFrame();

        if (frame == null || frame.getCamera().getTrackingState() !=  TrackingState.TRACKING){
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : updatedAugmentedImages){
            switch (augmentedImage.getTrackingState()){
                case PAUSED:
                    String text = "Detected image";
                    break;
                case TRACKING:
                    fitToScanView.setVisibility(View.GONE);

                    // Create an anchor for found image
                    if (!augmentedImageMap.containsKey(augmentedImage)){
                        AugmentedWebViewNode node = new AugmentedWebViewNode(this);
                        node.setImage(augmentedImage);
                        augmentedImageMap.put(augmentedImage, node);
                        arFragment.getArSceneView().getScene().addChild(node);
                    }
                    break;
                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    break;
            }
        }
    }

}
