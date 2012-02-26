package uk.co.halfninja.wallpaper.parallax;

import java.io.File;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

/**
 * TODO this setting screen utterly sucks
 */
public class ParallaxWallpaperSettings extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String CUSTOM_PATH_KEY = "custom_path";
    public static final String CUSTOM_PATH_ACTUAL_KEY = "custom_path_actual";
    private static final int REQ_CODE_PICK_IMAGE = 100001;
    private EditTextPreference customPathPreference;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setSharedPreferencesName(
                ParallaxWallpaper.SHARED_PREFS_NAME);
        addPreferencesFromResource(R.xml.settings);
        preferences = getPreferenceManager().getSharedPreferences();
        preferences.registerOnSharedPreferenceChangeListener(this);
        
        final String actualPath = preferences.getString(CUSTOM_PATH_ACTUAL_KEY, Environment.getExternalStorageDirectory().getAbsolutePath());
        
        if (preferences.getString(CUSTOM_PATH_KEY, null) == null) {
            
        }
        
        customPathPreference = (EditTextPreference) getPreferenceScreen().findPreference(CUSTOM_PATH_KEY);
        if (hasText(actualPath)) {
            customPathPreference.setSummary(actualPath);
            customPathPreference.setText(actualPath);
        }
        
        if (!hasText(customPathPreference.getText())) {
            resetPathPreference();
        }
        customPathPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            /**
             * Start any app that supports the openintents.org PICK_FILE intent,
             * such as File Manager
             */
            @Override public boolean onPreferenceClick(Preference preference) {
                Intent anyFile = new Intent(Intent.ACTION_GET_CONTENT);
                anyFile.setType("image/*");
                anyFile.addCategory(Intent.CATEGORY_OPENABLE);
                
                if (anyFile.resolveActivityInfo(getPackageManager(), 0) == null) {
                    return false;
                } else {
                    Intent chooser = Intent.createChooser(anyFile, "Pick the first layer");
                    startActivityForResult(chooser, REQ_CODE_PICK_IMAGE);
                    return true;
                }
            }
        });
        customPathPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return processNewPath(newValue);
            }
        });
        
    }
    
    private boolean processNewPath(Object newValue) {
        boolean alright = false;
        String value = (String) newValue;
        File file = new File(value);
        if (!file.exists()) {
            failedPathSet("File doesn't exist");
        } else if (!file.isFile()) {
            failedPathSet("Not a file");
        } else if (!file.canRead()) {
            failedPathSet("This file is unreadable");
        } else {
            Editor editor = preferences.edit();
            editor.putString(CUSTOM_PATH_ACTUAL_KEY, value);
            editor.commit();
            alright = true;
            customPathPreference.setSummary(value);
        }
        return alright;
    }

    private void failedPathSet(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
        resetPathPreference();
    }

    private void resetPathPreference() {
        customPathPreference.setText(Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    private boolean hasText(String text) {
        return text != null && text.trim().length() > 0;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
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
                    String filePath = cursor.getString(columnIndex);
                    
                    Log.i(ParallaxWallpaper.TAG, "Chose image " + filePath);
                    customPathPreference.setText(filePath);
                    customPathPreference.getEditText().setText(filePath);
                    processNewPath(filePath);
                } finally {
                    if (cursor != null) cursor.close();
                }

            }
            break;
        }
    }
}
