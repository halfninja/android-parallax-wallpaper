package uk.co.halfninja.wallpaper.parallax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

public class ParallaxWallpaperSettingsHome extends Activity {
	private static final String CUSTOM_PATH_KEY = "custom_path";
    private static final String CUSTOM_PATH_ACTUAL_KEY = "custom_path_actual";
    private static final int REQ_CODE_PICK_IMAGE = 100001;
    
    private final Context context = this; // for convenience in inner classes
    
    private EditTextPreference customPathPreference;
    private SharedPreferences preferences;

	private Button pickButton;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		
		pickButton = (Button)findViewById(R.id.selectLayersButton);
		pickButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
			Intent anyFile = new Intent(Intent.ACTION_GET_CONTENT);
            anyFile.setType("image/*");
            anyFile.addCategory(Intent.CATEGORY_OPENABLE);
            
            if (anyFile.resolveActivityInfo(getPackageManager(), 0) == null) {
                AlertDialog alert = new AlertDialog.Builder(context)
                	.setTitle("No activity to pick an image")
                	.setMessage(R.string.no_image_picker_alert_message);
                	.create();
            } else {
                Intent chooser = Intent.createChooser(anyFile, "Pick the first layer");
                startActivityForResult(chooser, REQ_CODE_PICK_IMAGE);
            }
		}});
	}
}
