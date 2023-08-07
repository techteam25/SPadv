package org.tyndalebt.storyproduceradv.tools.file;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.provider.OpenableColumns;
import java.io.InputStream;
import android.database.Cursor;
import android.content.ContentUris;
import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import java.io.File;
import android.os.Build;
import android.os.Environment;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import kotlin.text.Regex;

import android.provider.MediaStore;
import android.util.Log;

import org.tyndalebt.storyproduceradv.R;
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity;

public class UriUtils {
    private static Uri contentUri = null;

    @SuppressLint("NewApi")
    public static String getPathFromUri(final Context context, final Uri uri) {
        // check here to is it KITKAT or new version
        final boolean isKitKatOrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        String selection = null;
        String[] selectionArgs = null;
        // DocumentProvider
        if (isKitKatOrAbove && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                String fullPath = getPathFromUriData(context, uri, split);
                if (fullPath != "") {
                    return fullPath;
                } else {
                    return null;
                }
            }

            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final String id;
                    Cursor cursor = null;
                    try {
                        cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            String fileName = cursor.getString(0);
                            String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                            if (!TextUtils.isEmpty(path)) {
                                return path;
                            }
                        }
                    } finally {
                        if (cursor != null)
                            cursor.close();
                    }
                    id = DocumentsContract.getDocumentId(uri);
                    if (!TextUtils.isEmpty(id)) {
                        if (id.startsWith("raw:")) {
                            return id.replaceFirst("raw:", "");
                        }
                        String[] contentUriPrefixesToTry = new String[]{
                                "content://downloads/public_downloads",
                                "content://downloads/my_downloads"
                        };
                        for (String contentUriPrefix : contentUriPrefixesToTry) {
                            try {
                                final Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));

                                return getDataColumn(context, contentUri, null, null);
                            } catch (NumberFormatException e) {
                                //In Android 8 and Android P the id is not a number
                                return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                            }
                        }
                    }

                } else {
                    final String id = DocumentsContract.getDocumentId(uri);
                    if (id.startsWith("raw:")) {
                        return id.replaceFirst("raw:", "");
                    }
                    try {
                        contentUri = ContentUris.withAppendedId(
                                Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                    } catch (NumberFormatException e) {
                        FirebaseCrashlytics.getInstance().recordException(e);
                    }
                    if (contentUri != null) {
                        return getDataColumn(context, contentUri, null, null);
                    }
                }
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};


                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            } else if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri, context);
            }
        }
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }

            if (isGoogleDriveUri(uri)) {
                return getDriveFilePath(uri, context);
            }
            if( Build.VERSION.SDK_INT == Build.VERSION_CODES.N)
            {
                return getMediaFilePathForN(uri, context);
            }else
            {
                return getDataColumn(context, uri, null, null);
            }
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {  // used for unit tests
            File file = new File(uri.getPath());
            if (file.exists()) {
                return uri.getPath();
            }
            return null;
        }
        return null;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);

        return file.exists();
    }

    private static String getPathFromUriData(Context context, Uri uri, String[] pathData) {

        final String type = pathData[0];
        final String relativePath = "/" + pathData[1];
        String fullPath = "";

        String testPath = getStorageDir(context, uri);
        if (testPath != null) {
            // This will return an accurate path, but not necessarily a 
            // user friendly path
            return testPath + relativePath;
        }
        return "";
    }
    
    // RK 6-20-2023
    // Uses Android utilities to translate the uri to an
    // actual file system storage location.  StorageManager
    // contains the storage for internal and sdcard (at least in 
    // more recent versions of android) memory, but
    // not for external USB files.
    // DownloadActivity.getWorkDocStorageDir.......
    public static String getStorageDir(Context context, Uri uri) {
        try {
            // StorageManager covers internal drive and sdcard but not
            // external usb thumb drive
            StorageManager storage = context.getSystemService(StorageManager.class);
            List<StorageVolume> volumes = storage.getStorageVolumes();
            if ((volumes != null) && (volumes.size() > 0)) {
                int volumeNo = 0;
                String sdId = null;
                if (volumes.size() > 1) {
                    // which to use?
                    // is workdocfile in the primary memory or on the SD?
                    volumeNo = -1;
                    String segment = uri.getLastPathSegment();
                    boolean isPrimary = segment.indexOf("primary") == 0;
                    for (int i=0; i < volumes.size(); i++) {
                        if (isPrimary && volumes.get(i).isPrimary()) {
                            volumeNo = i;
                            break;
                        }
                        int index = segment.indexOf(':');
                        if (index > 0) {
                            // id should look like 1E0C-350C
                            sdId = segment.substring(0, index);
                            // toString should look like "StorageVolume:SDCARD(1E0C-350C)"
                            if (volumes.get(i).toString().indexOf(sdId) >= 0) {
                                volumeNo = i;
                                break;
                            }
                        }
                    }

                    if (volumeNo < 0) {
                        // this is not on the internal or sdcard
                        // check for a usb drive
                        return getUSBStorageDir(context, uri);
                    }
                }

                File file = null;
                try {
                    file = volumes.get(volumeNo).getDirectory();

                    // note that this file only include the sdcard id, not the full path

                    return file.getPath();
                } catch (Throwable ex) {
                    // StorageVolume.getDirectory() does not exist in Android 10 and earlier.
                    // try getPath() or disable the feature
                    return "/mnt/media_rw/" + sdId;
                }
            }
        }
        catch(Throwable ex){
            FirebaseCrashlytics.getInstance().recordException(ex);
        }
        return null;
    }

    // RK = 06-20-2023
    // this is called as a last resort if we could not find the uri
    // on either the internal memory or sdcard.
    private static String getUSBStorageDir(Context context, Uri uri) {
        try {
            // First, check that file actually exists.
            // if the file exists, then see if there is a usb drive installed.

            if (FileIO.getFileType(context, uri) != null) {  // check if file exists

                // the following will find a Usb storage device
                // but I am still looking for a way to connect the UsbDevice
                // to a specific file or directory URI.
                // If there are multiple usb devices, it is not clear
                // how to differentiate which is the correct one.
                UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
                Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
                while (deviceIterator.hasNext()) {
                    UsbDevice device = deviceIterator.next();
                    String deviceName = device.getDeviceName();
                    if ((device.getInterfaceCount() > 0) && (device.getInterface(0) != null) &&
                            (device.getInterface(0).getInterfaceClass() == UsbConstants.USB_CLASS_MASS_STORAGE)) {
                        return deviceName;  // + " " + device.getProductName() + " " + device.getDeviceClass() + " " + device.getInterfaceCount();
                    }
                }
            }
        }
        catch(Throwable ex){
            FirebaseCrashlytics.getInstance().recordException(ex);
        }
        return null;
    }

    private static String getDriveFilePath(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getCacheDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return file.getPath();
    }

    private static String getMediaFilePathForN(Uri uri, Context context) {
        Uri returnUri = uri;
        Cursor returnCursor = context.getContentResolver().query(returnUri, null, null, null, null);

        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();
        String name = (returnCursor.getString(nameIndex));
        String size = (Long.toString(returnCursor.getLong(sizeIndex)));
        File file = new File(context.getFilesDir(), name);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);
            int read = 0;
            int maxBufferSize = 1 * 1024 * 1024;
            int bytesAvailable = inputStream.available();

            //int bufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);

            final byte[] buffers = new byte[bufferSize];
            while ((read = inputStream.read(buffers)) != -1) {
                outputStream.write(buffers, 0, read);
            }
            Log.e("File Size", "Size " + file.length());
            inputStream.close();
            outputStream.close();
            Log.e("File Path", "Path " + file.getPath());
            Log.e("File Size", "Size " + file.length());
        } catch (Exception e) {
            Log.e("Exception", e.getMessage());
        }
        return file.getPath();
    }

    private static String getDataColumn(Context context, Uri uri,
                                        String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);

            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private static boolean isGoogleDriveUri(Uri uri) {
        return "com.google.android.apps.docs.storage".equals(uri.getAuthority()) || "com.google.android.apps.docs.storage.legacy".equals(uri.getAuthority());
    }

    public static String getUIPathText(MainBaseActivity activity, Uri uri) {
        // translates the uri path to a ui string to display for the copy folder
        if (!FileIO.fileExists(activity, uri)) {
            return null;  // file does not exist
        }
        return getUIPathTextAlways(activity, uri);
    }

    public static String getUIPathTextAlways(MainBaseActivity activity, Uri uri) {

        String uriStr = UriUtils.getPathFromUri(activity, uri);
        if (uriStr != null) {
            String replaceStr = getStorageText(activity, uri, uriStr);
            uriStr = activity.getUIPathTextInternal(uriStr, replaceStr);
            Uri uiUri = Uri.parse(uriStr);
            return uiUri.getPath();
        }
        return null;
    }

    private static String getUIPathTextInternal(Context context, Uri uri, String uriStr) {

        // At this point the videoFileUriStr will look something like this: /storage/emulated/0/
        // This is the actual path. However, it needs be changed to the SD Card (/sdcard/)
        // which is a symbolic link to the emulated storage path.
        // sdcard/: Is a symlink to...
        //      /storage/sdcard0 (Android 4.0+)
        // In Story Publisher Adv, the version will never be less than Android 4.0
        // We will instead show it as an optional [sdcard]
        // The below code will change: /storage/emulated/0/ to /storage/[sdcard]/
        String replaceStr = getStorageText(context, uri, uriStr);
        Regex regex = new Regex("(/storage\\/emulated\\/)\\d+");
        String retVal = uriStr.replace(regex.toString(), replaceStr);

        // Also, the SD-Card could show up as /storage/####-####/ where # is a hexidecimal value
        Regex regex2 = new Regex("(/storage)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}");
        retVal = retVal.replace(regex2.toString(), replaceStr);

        // Also, the SD-Card could show up as /mnt/media_rw/####-####/ where # is a hexidecimal value for earlier android releases
        Regex regex3 = new Regex("(/mnt/media_rw)\\/[0-9a-fA-F]{4}-[0-9a-fA-F]{4}");
        retVal = retVal.replace(regex3.toString(), replaceStr);

        // Also, this is for a usb memory stick
        Regex regex4 =  new Regex("(/dev/bus/usb)\\/[0-9]{3}\\/[0-9]{3}");
        retVal = retVal.replace(regex4.toString(), replaceStr);
        return retVal;
    }

    public static String getStorageText(Context context, Uri uri, String uriStr) {
        String segment = uri.getLastPathSegment();
        boolean isPrimary = (segment.indexOf("primary") == 0) ||
                (segment.indexOf("raw") == 0);
        if (isPrimary) {
            return "[" + context.getString(R.string.internal) + "]";
        }
        else if (uriStr.indexOf("usb") >= 0) {
            return "[" + context.getString(R.string.external) + "]";
        }
        return "[" + context.getString(R.string.sdcard) + "]";
    }

}
