package uk.co.halfninja.wallpaper.parallax;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.rbgrn.android.glwallpaperservice.*;

import android.content.SharedPreferences;
import android.util.Log;

/**
 * Draws a wallpaper much like the regular wallpapers which slide behind
 * the home screen with a parallax effect. The difference with this wallpaper
 * is that it supports multiple images with different widths, for a much more
 * intricate-looking parallax effect.
 * 
 * The main thing is that it's configurable, so you can use any set of images.
 */
public class ParallaxWallpaper extends GLWallpaperService {

    public static final String SHARED_PREFS_NAME="parallaxwallpaper_settings";
    public static final String TAG = "ParallaxWallpaper";
    
    private SharedPreferences mPrefs;
	private ParallaxWallpaperRenderer renderer;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        return new ParallaxEngine();
    }
/*    
//    static class Layer {
//        public Bitmap bitmap;
//        private float scale = 1.0f;
//        private Matrix matrix = new Matrix();
//        public Layer(Bitmap b) {
//            this.bitmap = b;
//        }
//        public void setScale(float factor) {
//            scale = factor;
//        }
//        public Matrix getMatrix(float x, float y) {
//            if (scale == 1) {
//                matrix.reset();
//            } else {
//                matrix.setScale(scale, scale);
//            }
//            matrix.postTranslate(x, y);
//            return matrix;
//        }
//    }
*/    
    public static List<String> findLayers(String path) {
    	List<String> files = new ArrayList<String>();
        Pattern p = Pattern.compile("(.+)([1-9]\\d*)(\\..+)");
        Matcher matcher = p.matcher(path);
        File file = new File(path);
        if (file.exists()) { 
            if (matcher.matches()) {
                String prefix = matcher.group(1);
                String suffix = matcher.group(3);
                for (int i=1; i<9; i++) {
                    File f = new File(prefix + i + suffix);
                    if (f.isFile()) {
                        files.add(f.getAbsolutePath());
                    } else {
                        break;
                    }
                }
            } else {
                Log.i(TAG, "Filename didn't end in a number - just using the one layer.");
                files.add(path);
            }
        }
    	return files;
    }

    class ParallaxEngine extends GLEngine {

        ParallaxEngine() {
        	super();
            mPrefs = ParallaxWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            renderer = new ParallaxWallpaperRenderer();
            renderer.setContext(ParallaxWallpaper.this);
            setRenderer(renderer);
            setRenderMode(RENDERMODE_WHEN_DIRTY);
            onSharedPreferenceChanged(mPrefs, null);
            requestRender();
        }
        
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            String customPath = prefs.getString(ParallaxWallpaperSettings.CUSTOM_PATH_ACTUAL_KEY, null);
            Log.d(TAG, "customPath: " + customPath);
            if (customPath != null) {
                renderer.setLayerFiles(findLayers(customPath));
            }
        }

        public void onDestroy() {
        	super.onDestroy();
        	if (renderer != null) {
                renderer.release();
            }
            renderer = null;
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            if (renderer != null) {
            	renderer.setOffset(xOffset);
            }
            requestRender();
        }
    }
}
