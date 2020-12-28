package ng.com.curious.augmentedimages;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.rendering.ViewSizer;

import java.util.concurrent.CompletableFuture;

public class AugmentedWebViewNode extends AnchorNode {

    private static final String TAG = "AugmentedImageNode";

    private Node webViewNode;

    private static CompletableFuture<ViewRenderable> webViewRenderable;

    public AugmentedWebViewNode(Context context){
        if (webViewRenderable == null)
            webViewRenderable = ViewRenderable.builder().setView(context, R.layout.web_view).build();
    }


    /**
     * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
     * created based on an Anchor created from the image. The corners are then positioned based on the
     * extents of the image. There is no need to worry about world coordinates since everything is
     * relative to the center of the image, which is the parent node of the corners.
     */
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void setImage(AugmentedImage image) {
        ViewRenderable modelRenderable;
        // Set the anchor based on the center of the image.
        setAnchor(image.createAnchor(image.getCenterPose()));
        if (!webViewRenderable.isDone()) {
            CompletableFuture.allOf(webViewRenderable)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        } else {
        }

        webViewNode = new Node();
        webViewNode.setParent(this);
        webViewNode.setLocalPosition(new Vector3(0.0f, 0.0f, 0.0f));
        webViewNode.setRenderable(webViewRenderable.getNow(null));
        webViewNode.setLocalRotation(new Quaternion(90f, 0f, 0f, -90f));

        if (webViewRenderable.isDone()) {
            modelRenderable = webViewRenderable.getNow(null);
            modelRenderable.setSizer(new ViewSizer() {
                @Override
                public Vector3 getSize(View view) {
                    return new Vector3(0.25f, 0.35f, 0.0f);
                }
            });
            WebView webView = (WebView) modelRenderable.getView().findViewById(R.id.webView);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return false;
                }
            });
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webView.loadUrl("https://www.youtube.com/watch?v=qiA7ZcfcD2o");
        }
    }
}
