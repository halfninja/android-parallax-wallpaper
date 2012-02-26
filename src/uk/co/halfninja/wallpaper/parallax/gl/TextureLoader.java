package uk.co.halfninja.wallpaper.parallax.gl;

import java.io.IOException;

import javax.microedition.khronos.opengles.GL10;
import static javax.microedition.khronos.opengles.GL10.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.util.Log;
import static uk.co.halfninja.wallpaper.parallax.ParallaxWallpaper.TAG;

public class TextureLoader {
	public static final int MAX_TEXTURES = 16;

	private final Capabilities capabilities;
	
	public TextureLoader(Capabilities capabilities) {
		super();
		this.capabilities = capabilities;
	}

	private int[] textures = new int[MAX_TEXTURES];
	private int nextSlot = 0;
	
	public boolean slotsAvailable() {
		return nextSlot < MAX_TEXTURES-1;
	}
	
	public Texture loadTextureFromFile(GL10 gl, String filename) throws IOException {
		Bitmap originalBitmap, bitmap;
		originalBitmap = bitmap = BitmapFactory.decodeFile(filename);
		if (bitmap == null) {
			throw new IOException("Couldn't load bitmap " + filename);
		}
		
		Log.d(TAG, "Loaded bitmap: (" + bitmap.getWidth() +","+bitmap.getHeight()+")");
		
		if (!capabilities.supportsNonPowerOfTwoTextures()) {
			bitmap = BitmapUtils.createPowerOfTwoBitmap(bitmap);
		}
		
		Texture t = loadTexture(gl, bitmap);
		// in case we expanded the bitmap to fit the canvas,
		t.setBitmapWidth(originalBitmap.getWidth());
		t.setBitmapHeight(originalBitmap.getHeight());
		Log.d(TAG, "Loaded texture from file: " + t.toString());
		bitmap.recycle();
		originalBitmap.recycle();
		return t;
	}
	
	public Texture loadTexture(GL10 gl, Bitmap bitmap) {		
		// generate one texture pointer
		gl.glGenTextures(1, textures, nextSlot);
		// ...and bind it to our array
		int id = textures[nextSlot];
		gl.glBindTexture(GL10.GL_TEXTURE_2D, id);

		// create nearest filtered texture
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

		// Use Android GLUtils to specify a two-dimensional texture image from our bitmap
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);

		int error = gl.glGetError();
		if (error != 0) {
			Log.e(TAG, "Error loading GL texture. OpenGL code: " + error);
		}
		
		// Clean up
		nextSlot++;
		Texture t = new Texture(id);
		t.setTexWidth(bitmap.getWidth());
		t.setBitmapWidth(bitmap.getWidth());
		t.setTexHeight(bitmap.getHeight());
		t.setBitmapHeight(bitmap.getHeight());
		bitmap.recycle();
		return t;
	}
	
	void logError(GL10 gl) {
		int error = gl.glGetError();
		String msg;
		switch (error) {
			case GL_INVALID_VALUE: msg="invalid value"; break;
			default: msg = ""+error; break;
		}
		Log.d(TAG, "gl error "+msg);
	}

	public void clear(GL10 gl) {
		gl.glDeleteTextures(MAX_TEXTURES, textures, 0);
		nextSlot = 0;
	}
}
