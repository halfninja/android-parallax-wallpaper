package uk.co.halfninja.wallpaper.parallax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

/**
 * Draws a wallpaper much like the regular wallpapers which slide behind
 * the home screen with a parallax effect. The difference with this wallpaper
 * is that it supports multiple images with different widths, for a much more
 * intricate-looking parallax effect.
 * 
 * The main thing is that it's configurable, so you can use any set of images.
 */
public class ParallaxWallpaper extends WallpaperService {

    public static final String SHARED_PREFS_NAME="parallaxwallpaper_settings";

    public static final String TAG = "ParallaxWallpaper";
    
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
    
    static class Layer {
        public Bitmap bitmap;
        private float scale = 1.0f;
        private Matrix matrix = new Matrix();
        public Layer(Bitmap b) {
            this.bitmap = b;
        }
        public void setScale(float factor) {
            scale = factor;
        }
        public Matrix getMatrix(float x, float y) {
            if (scale == 1) {
                matrix.reset();
            } else {
                matrix.setScale(scale, scale);
            }
            matrix.postTranslate(x, y);
            return matrix;
        }
    }
    
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

    class ParallaxEngine extends Engine 
        implements SharedPreferences.OnSharedPreferenceChangeListener {


        private final Handler mHandler = new Handler();

        private float mOffset;
        
        private List<Layer> layers = new ArrayList<Layer>();
        private List<String> layerFiles = new ArrayList<String>();

        private final Runnable mDraw = new Runnable() {
            public void run() {
                drawFrame();
            }
        };
        private SharedPreferences mPrefs;

        private Rect mFrame;

        private int mHeight;

        ParallaxEngine() {
            mPrefs = ParallaxWallpaper.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
            mPrefs.registerOnSharedPreferenceChangeListener(this);
        }
        
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            String customPath = prefs.getString(ParallaxWallpaperSettings.CUSTOM_PATH_ACTUAL_KEY, null);
            Log.d(TAG, "customPath: " + customPath);
            if (customPath != null) {
                layerFiles.clear();
                layerFiles.addAll(findLayers(customPath));
                Log.d(TAG, "Done finding layers, now to load them up");
                loadLayers();
            }
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            setTouchEventsEnabled(false);
            onSharedPreferenceChanged(mPrefs, null);
        }

        private void loadLayers() {
            try {
                clearLayers();
                for (String file: layerFiles) {
                    addLayer(file);
                }
                recalibrateLayers();
            } catch (IOException e) {
                Log.e(TAG, "I/O error loading wallpaper", e);
                layers.clear();
                Toast.makeText(ParallaxWallpaper.this, "There was a problem loading the parallax wallpaper.", Toast.LENGTH_LONG).show();
            } catch (OutOfMemoryError oom) {
                Log.e(TAG, "Ran out of memory while loading wallpaper.", oom);
            	layers.clear();
            	Toast.makeText(ParallaxWallpaper.this, "Whoops, we ran out of memory trying to load the images. Try using fewer layers, or resize them to be smaller.", Toast.LENGTH_LONG).show();
            }
        }
        
        private void addLayer(String name) throws IOException {
            Bitmap layer = BitmapFactory.decodeFile(name);
            if (layer == null) {
                throw new IOException("BitmapFactory couldn't decode asset " + name);
            }
            synchronized(layers) {
                layers.add(new Layer(layer));
            }
        }
        
        private void clearLayers() {
            synchronized(layers) {
                layers.clear();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            mHandler.removeCallbacks(mDraw);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            if (visible) {
                drawFrame();
            } else {
                mHandler.removeCallbacks(mDraw);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            
            mHeight = height;
            
            recalibrateLayers();
            
            drawFrame();
        }

        /**
         * Adjust the scale matrix for each bitmap so that they match
         * the height of the screeen.
         * 
         * TODO we don't currently check if the width of the bitmap
         * covers the screen, so it could look stupid.
         */
        private void recalibrateLayers() {
            for (Layer layer : layers) {
                final int bitmapHeight = layer.bitmap.getHeight();
                layer.setScale((float)mHeight / (float)bitmapHeight);
            }
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            mHandler.removeCallbacks(mDraw);
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                float xStep, float yStep, int xPixels, int yPixels) {
            mOffset = xOffset;
            drawFrame();
        }
     
        void drawFrame() {
            final SurfaceHolder holder = getSurfaceHolder();
            final Rect frame = holder.getSurfaceFrame();

            mFrame = frame;
            
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    // draw something
                    drawNonsense(c);
                }
            } finally {
                if (c != null) holder.unlockCanvasAndPost(c);
            }

        }

        private void drawNonsense(Canvas c) {
            int frameWidth = mFrame.width();
            
            for (int i=layers.size()-1; i>=0; i--) {
                Layer layer = layers.get(i);
                Bitmap bitmap = layer.bitmap;
                float bitmapWidth = bitmap.getWidth() * layer.scale;
                float max = frameWidth - bitmapWidth;
                float offset = mOffset * max;
                
                final Matrix m = layer.getMatrix(offset, 0);
                
                // TODO tile to fit width when narrower than screen.
                c.drawBitmap(bitmap, m, null);
            }
        }

        
    }
}
