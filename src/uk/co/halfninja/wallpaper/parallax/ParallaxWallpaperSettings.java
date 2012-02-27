package uk.co.halfninja.wallpaper.parallax;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import static uk.co.halfninja.wallpaper.parallax.ParallaxWallpaper.TAG;

public class ParallaxWallpaperSettings extends Activity implements OnSharedPreferenceChangeListener {
    public static final String CUSTOM_PATH_ACTUAL_KEY = "custom_path_actual";
    private static final int REQ_CODE_PICK_IMAGE = 100001;
    
    private final Context context = this; // for convenience in inner classes

    private SharedPreferences preferences;
	private Button pickButton;
	private TextView selectedImage;
	
	public String getLayerPath() {
		return preferences.getString(CUSTOM_PATH_ACTUAL_KEY, null);
	}
	
	public void setLayerPath(String path) {
		preferences.edit().putString(CUSTOM_PATH_ACTUAL_KEY, path).commit();
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		preferences = getSharedPreferences(ParallaxWallpaper.SHARED_PREFS_NAME, 0);
		preferences.registerOnSharedPreferenceChangeListener(this);
		
		selectedImage = (TextView)findViewById(R.id.selectedImage);
		updateSelectedImageUi();
		
		pickButton = (Button)findViewById(R.id.selectLayersButton);
		pickButton.setOnClickListener(new View.OnClickListener() { public void onClick(View v) {
			Intent anyFile = new Intent(Intent.ACTION_GET_CONTENT);
            anyFile.setType("image/*");
            anyFile.addCategory(Intent.CATEGORY_OPENABLE);
            if (anyFile.resolveActivityInfo(getPackageManager(), 0) == null) {
                new AlertDialog.Builder(context)
                	.setTitle(R.string.no_image_picker_alert_title)
                	.setMessage(R.string.no_image_picker_alert_message)
                	.create().show();
            } else {
                Intent chooser = Intent.createChooser(anyFile, "Pick the first layer");
                startActivityForResult(chooser, REQ_CODE_PICK_IMAGE);
            }
		}});
	}
	
	private void updateSelectedImageUi() {
		String value = getLayerPath();
		if (value != null) {
			selectedImage.setVisibility(View.VISIBLE);
			// TODO i18n
			selectedImage.setText(Html.fromHtml("Currently chosen: <br><b>" + value +"</b>"));
		}
	}

	private void processNewPath(String filePath) {
		Log.d(TAG, "new file path " + filePath);
		setLayerPath(filePath);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		Log.d(TAG, "onSharedPreferenceChanged("+key+")");
		if (key.equals(CUSTOM_PATH_ACTUAL_KEY)) {
			updateSelectedImageUi();
		}
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) { 
        super.onActivityResult(requestCode, resultCode, returnedIntent); 
        switch(requestCode) { 
	        case REQ_CODE_PICK_IMAGE:
	            if(resultCode == RESULT_OK){  
	                Uri selectedImage = returnedIntent.getData();
	                Log.i(ParallaxWallpaper.TAG, "Picked image " + selectedImage);
	                String[] filePathColumn = {MediaStore.Images.Media.DATA};
	
	                Cursor cursor = null;
	                try {
	                    cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
	                    cursor.moveToFirst();
	                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
	                    final String filePath = cursor.getString(columnIndex);
	                    processNewPath(filePath);
	                } finally {
	                    if (cursor != null) cursor.close();
	                }
	
	            }
	            break;
        }
    }
	
}
