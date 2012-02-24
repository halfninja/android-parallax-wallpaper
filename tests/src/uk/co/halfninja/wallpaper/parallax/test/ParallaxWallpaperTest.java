package uk.co.halfninja.wallpaper.parallax.test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import uk.co.halfninja.wallpaper.parallax.ParallaxWallpaper;
import android.os.Environment;
import android.test.AndroidTestCase;

public class ParallaxWallpaperTest extends AndroidTestCase {
	public void testFindLayers() throws Exception {
		File data = getContext().getFilesDir();
		String dataPath = data.getAbsolutePath();
		for (String name: new String[] {
				"mylayer0.png", // ignored
				"mylayer1.png",
				"mylayer2.png",
				"mylayer3.png",
				"something4.png", //ignored
		}) {
			new File(data, name).createNewFile();
		}
 		
		List<String> layers = ParallaxWallpaper.findLayers(new File(data, "mylayer2.png").getAbsolutePath());
		assertEquals(Arrays.asList(new String[]{
				dataPath+"/mylayer1.png",
				dataPath+"/mylayer2.png",
				dataPath+"/mylayer3.png"
		}), layers);
	}
}
