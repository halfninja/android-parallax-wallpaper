package uk.co.halfninja.wallpaper.parallax;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import uk.co.halfninja.wallpaper.parallax.gl.Capabilities;
import uk.co.halfninja.wallpaper.parallax.gl.Quad;
import uk.co.halfninja.wallpaper.parallax.gl.Texture;
import uk.co.halfninja.wallpaper.parallax.gl.TextureLoader;
import uk.co.halfninja.wallpaper.parallax.gl.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService.Renderer;
import static uk.co.halfninja.wallpaper.parallax.ParallaxWallpaper.TAG;
import static javax.microedition.khronos.opengles.GL10.*;

public final class ParallaxWallpaperRenderer implements Renderer {

	private float offset = 0.0f;
	private int height;
	private int width;
	private boolean visible;
	private Context context;
	
	private Capabilities capabilities = new Capabilities();
	private TextureLoader textureLoader = new TextureLoader(capabilities);
//	private String bitmapPath;
//	private Texture tex;
	private List<Quad> layers = new ArrayList<Quad>();
	private List<String> layerFiles = new ArrayList<String>();
	
	private GL10 gl;
	
//	public void setBitmapPath(String bitmapPath) {
//		this.bitmapPath = bitmapPath;
//	}

	public void setContext(Context context) {
		this.context = context;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		gl.glClearColor(offset, 0.4f, 0.2f, 1f);
        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        gl.glColor4f(1f, 1f, 1f, 1f);
        for (Quad quad : layers) {
        	quad.setX(offset * (width-quad.getWidth()));
        	quad.draw(gl);
        }
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		width = w;
		height = h;
		Utils.pixelProjection(gl, w, h);
		gl.glEnable(GL_TEXTURE_2D);
		gl.glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
		gl.glEnable (GL_BLEND);
		gl.glBlendFunc (GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		
		resizeLayers();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig cfg) {
		this.gl = gl;
		capabilities.reload(gl);
		
		try {
			reloadLayers();
		} catch (IOException e) {
			Log.e(TAG, "Error loading textures", e);
			//Toast.makeText(context, "Error loading layers.", Toast.LENGTH_LONG).show();
		}
	}

	public void reloadLayers() throws IOException {
		if (gl != null) {
			layers.clear();
			textureLoader.clear(gl);
			for (String bitmapPath : layerFiles) {
				Quad quad = new Quad();
				Texture tex = textureLoader.loadTextureFromFile(gl, bitmapPath);
				Log.i(TAG, "Loaded texture " + tex.id);
				quad.setTexture(tex);
				layers.add(0, quad);
			}
		}
	}

	public void setOffset(float xOffset) {
		offset = xOffset;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public void release() {
    }
	
	public void resizeLayers() {
		for (Quad quad : layers) {
			int bitmapHeight = quad.getTexture().getBitmapHeight();
//			Log.d(TAG, "Scaling quad with texture " + quad.getTexture());
			float ratio = (float)height/bitmapHeight;
//			Log.d(TAG, "Scale ratio " + ratio + " so that width is " + (quad.getTexture().getBitmapWidth() * ratio));
			quad.setHeight(height);
			quad.setWidth(quad.getTexture().getBitmapWidth() * ratio);
		}
	}

	public void setLayerFiles(List<String> files) {
		layerFiles = files;
	}
}
