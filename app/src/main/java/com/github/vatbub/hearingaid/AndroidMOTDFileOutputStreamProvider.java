package com.github.vatbub.hearingaid;

import android.app.Activity;
import android.content.Context;

import com.github.vatbub.common.view.motd.MOTDFileOutputStreamProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by frede on 14.01.2018.
 */

public class AndroidMOTDFileOutputStreamProvider implements MOTDFileOutputStreamProvider {
    private Activity activity;
    public AndroidMOTDFileOutputStreamProvider(Activity activity){
        setActivity(activity);
    }

    @Override
    public FileOutputStream createFileOutputStream(String fileName) throws FileNotFoundException {
        return getActivity().openFileOutput(fileName, Context.MODE_PRIVATE);
    }

    @Override
    public File getMOTDFolder() {
        return getActivity().getFilesDir();
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }
}
