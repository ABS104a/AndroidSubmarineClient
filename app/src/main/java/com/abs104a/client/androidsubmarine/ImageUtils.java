package com.abs104a.client.androidsubmarine;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by ABS104a on 2017/04/09.
 */

public class ImageUtils {

    public void saveBitmap(Context context,Bitmap saveImage) throws IOException {

        final String SAVE_DIR = "/Submarine/";
        File file = new File(Environment.getExternalStorageDirectory().getPath() + SAVE_DIR);
        try{
            Log.v("File","dir:"+ file.getAbsolutePath());
            if(!file.exists()){
                file.mkdir();
                file.createNewFile();
            }
        }catch(SecurityException e){
            e.printStackTrace();
            throw e;
        }

        Date mDate = new Date();
        SimpleDateFormat fileNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName = fileNameDate.format(mDate) + ".jpg";
        String AttachName = file.getAbsolutePath() + "/" + fileName;

        try {
            File newFile = new File(AttachName);
            FileOutputStream out = new FileOutputStream(newFile);
            saveImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch(IOException e) {
            e.printStackTrace();
            throw e;
        }

        // save index
        ContentValues values = new ContentValues();
        ContentResolver contentResolver = context.getContentResolver();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.TITLE, fileName);
        values.put("_data", AttachName);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static Bitmap copy(Context context, Bitmap bitmap)
            throws IOException {
        File file = File.createTempFile("_ub", ".tmp", context.getCacheDir());
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        FileChannel channel = randomAccessFile.getChannel();
        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, width
                * height * 4);
        bitmap.copyPixelsToBuffer(map);
        Bitmap mutable = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        map.position(0);
        mutable.copyPixelsFromBuffer(map);
        channel.close();
        randomAccessFile.close();

        file.delete();

        return mutable;
    }
}
