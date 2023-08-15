package org.tyndalebt.storyproduceradv.activities;

import 	android.os.storage.StorageManager;
import 	android.os.storage.StorageVolume;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.content.Intent;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.tyndalebt.storyproduceradv.BuildConfig;
import org.tyndalebt.storyproduceradv.R;

import org.tyndalebt.storyproduceradv.controller.BaseController;
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadAdapter;
import org.tyndalebt.storyproduceradv.controller.adapter.DownloadDS;
import org.tyndalebt.storyproduceradv.model.Workspace;
import org.tyndalebt.storyproduceradv.tools.file.*;

// This needs to correspond to name on server

public class DownloadActivity extends BaseActivity {
    public static final String BLOOM_LIST_FILE = "BloomfileLang";

    private static String file_url;
    public ProgressBar pBar;
    public TextView pText;
    public ListView pView;
    public ImageView pDownloadImage;
    public static final int progress_bar_type = 0;
    public DownloadFileFromURL at;
    public DrawerLayout mDrawerLayout = null;
    // First pass is to parse languages, select the language, second pass is to choose story within that language
    public Boolean firstPass;
    public String chosenLanguage;
    public String bloomFileContents;
    ArrayList<HashMap<String, String>> formList = new ArrayList<HashMap<String, String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_download);

        pText = (TextView) findViewById(R.id.pProgressText);
        pBar = (ProgressBar) findViewById(R.id.progressBar);

        BuildNativeLanguageArray();
        at = new DownloadFileFromURL(this);
        at.progress_bar_type = this.progress_bar_type;
        file_url = BuildConfig.ROCC_URL_PREFIX + "/Files/Bloom/";
        at.execute(file_url + BLOOM_LIST_FILE);
        firstPass = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_with_help, menu);
        return true;
    }

    // First build a list of distinct languages - use json file to show native versions of language
    // Second pass, build list of story names that have the chosen language as the prefix
    public void buildBloomList(String pList[], String pURL[]) {
        setContentView(R.layout.bloom_list_container);

        Toolbar mActionBarToolbar = findViewById(R.id.toolbarMoreTemplates);
        ActionBar supportActionBar;
        setSupportActionBar(mActionBarToolbar);
        supportActionBar = getSupportActionBar();
        supportActionBar.setTitle(R.string.more_templates);
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);

        mDrawerLayout = findViewById(R.id.drawer_layout_bloom);
        //Lock from opening with left swipe
        mDrawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        NavigationView nav  = (NavigationView)findViewById(R.id.nav_view_bloom);
        nav.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                return NavItemSelected(item);
            }
        });

        pView = (ListView) findViewById(R.id.bloom_list_view);
        pDownloadImage = (ImageView) findViewById(R.id.image_download);

        ArrayList<DownloadDS> arrayList = new ArrayList<>();
        Integer idx;
        String tmp;

        // array of all possible languages listed in the desired order with blank URL
        if (firstPass) {
            for (idx = 0; idx < formList.size(); idx++) {
                arrayList.add(new DownloadDS(formList.get(idx).get("displayname"), "", false));
            }
        }

        for (idx = 0; idx < pList.length; idx++) {
            if (!folderExists(this, pURL[idx])) {
                if (pURL[idx].equals("Language")) {
                    tmp = getNativeLangName(pList[idx]);
                    if (!isLanguageRoman(pList[idx])) {
                        tmp = tmp + " / " + at.removeExtension(pList[idx]);
                    }
                } else {
                    tmp = at.removeExtension(pList[idx]);
                }
                if (firstPass) {
                    // Find the right place to put it in the array
                    Integer idx1;
                    DownloadDS ds;
                    for (idx1 = 0; idx1 < arrayList.size(); idx1++) {
                        ds = arrayList.get(idx1);
                        if (ds.getName() == getNativeLangName(pList[idx])) {
                            // Found at least one file that can be downloaded in this language
                            // Replace it with an entry that has a URL string
                            arrayList.remove(ds);
                            arrayList.add(idx1, new DownloadDS(tmp, pURL[idx], false));
                        }
                    }
                } else {
                    arrayList.add(new DownloadDS(tmp, pURL[idx], false));
                }
            }
        }

        if (firstPass) {
            //  Go through array and remove any that have an empty string for URL, indicating that language does not any files to download
            for (idx = 0; idx < arrayList.size(); idx++) {
                while (idx < arrayList.size() && arrayList.get(idx).getURL() == "") {
                    arrayList.remove(arrayList.get(idx));
                    arrayList.trimToSize();
                }
            }
            // arrayList now has a list of languages that have stories to download, in the desired order
        }
        DownloadAdapter arrayAdapter = new DownloadAdapter(arrayList, this);
        pView.setAdapter(arrayAdapter);

        pDownloadImage.setOnClickListener(clickListener);
    }

    public String loadJSONFromAsset(String pFilename) {
        String json = null;
        try {
            InputStream is = getAssets().open(pFilename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            // ex.printStackTrace();
            return null;
        }
        return json;
    }

    public String getNativeLangName(String pLanguage) {
        String nativeLang = null;

        for (int i = 0; i < formList.size(); i++) {
            if (formList.get(i).get("filename").equals(pLanguage)) {
                nativeLang = formList.get(i).get("displayname");
                break;
            }
        }
        return nativeLang;
    }

    public String getEnglishLangName(String pLanguage) {
        String englishLang = null;

        for (int i = 0; i < formList.size(); i++) {
            if (formList.get(i).get("displayname").equals(pLanguage)) {
                englishLang = formList.get(i).get("filename");
                break;
            }
        }
        return englishLang;
    }

    public Boolean isLanguageRoman(String pLanguage) {
        String nativeLang = null;

        for (int i = 0; i < formList.size(); i++) {
            if (formList.get(i).get("filename").equals(pLanguage)) {
                if (formList.get(i).get("roman").equals("yes")) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public void BuildNativeLanguageArray() {
        try {
            JSONObject obj = new JSONObject(loadJSONFromAsset("TemplateLanguages.json"));
            JSONArray m_jArry = obj.getJSONArray("language");
            HashMap<String, String> m_li;

            for (int i = 0; i < m_jArry.length(); i++) {
                JSONObject jo_inside = m_jArry.getJSONObject(i);
                String formula_value = jo_inside.getString("filename");
                String url_value = jo_inside.getString("displayname");
                String roman_value = jo_inside.getString("roman");

                //Add your values in your `ArrayList` as below:
                m_li = new HashMap<String, String>();
                m_li.put("filename", formula_value);
                m_li.put("displayname", url_value);
                m_li.put("roman", roman_value);

                formList.add(m_li);
            }
        } catch (JSONException e) {
            // e.printStackTrace();
        }
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        public void onClick(View v) {
            if (v.equals(pDownloadImage)) {
                // Check for enough empty space
                checkAvailableSpaceThenDownload();
            }
        }
    };

    /**
     * RK 04/18/2023
     * Issue #98:  Evaluate space on phone before download templates
     *
     * Test how much free space is on the phone before downloading templates.
     * Checks for free space available, at 1gb per selected template or at
     * least 4gb.  If the space is below the minimun give them a warning dialog with
     * how much space they have and recommend to them to free up some space
     * before proceeding. If the user has selected more than 4 templates to
     * download, the minimum space is calculated as 1gb per template.

     * Note: The slightly tricky part of this feature is to determine whether the
     * space we need is from the SD card or the main memory.  To do that, we
     * utilizes the API StorageVolume.getDirectory() which does
     * not exist in Android 10 and earlier.  So the feature is disabled for those
     * platforms.
     */
    private void checkAvailableSpaceThenDownload() {
        // Build urlList then download
        String urlList[] = BuildURLList();
        if (urlList != null) {
            File dir = getWorkDocStorageDir();
            long size = getAvailableSpace(dir);
            if (size >= 0) {  // getAvailableSpace returns -1 for Android 10 and earlier
                size = size / 1000000; // in mbytes

                long minSize = urlList.length * 1000;  // in mb
                minSize = (minSize < 4000) ? 4000 : minSize;

                if (size < minSize) {
                // if (size > 0) {  // For debug

                    DialogInterface.OnClickListener onClick = new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dlg, int value) {
                            dlg.dismiss();
                            if (value == -1) {  // cancel is value == -2
                                doDownload(urlList);
                            }
                        }
                    };

                    String spaceMsg = getString(R.string.template_space_message) + getSizeMessage(size);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.template_space_title));
                    builder.setMessage(spaceMsg);
                    builder.setNegativeButton(getString(R.string.cancel), onClick);
                    builder.setPositiveButton(getString(R.string.ok), onClick);
                    AlertDialog dlg = builder.create();
                    dlg.show();
                } else {  // if enough space
                    doDownload(urlList);
                }
            } else {  // if indeterminant, do the download
                doDownload(urlList);
            }
        }
    }

    public File getWorkDocStorageDir() {
        try {
            StorageManager storage = getSystemService(StorageManager.class);
            List<StorageVolume> volumes = storage.getStorageVolumes();
            if ((volumes != null) && (volumes.size() > 0)) {
                int volumeNo = 0;
                if (volumes.size() > 1) {
                    // which to use?
                    // is workdocfile in the primary memory or on the SD?
                    volumeNo = -1;
                    String segment = Workspace.INSTANCE.getWorkDocFile().getUri().getLastPathSegment();
                    boolean isPrimary = segment.indexOf("primary") == 0;
                    for (int i=0; i < volumes.size(); i++) {
                        if (isPrimary && volumes.get(i).isPrimary()) {
                            volumeNo = i;
                            break;
                        }
                        if (segment.indexOf(volumes.get(i).getDirectory().getName() +':') == 0) {
                            volumeNo = i;
                            break;
                        }
                    }

                    if (volumeNo < 0) {
                        return null;
                    }
                }

                File file = null;
                try {
                    file = volumes.get(volumeNo).getDirectory();
                    return file;
                } catch (Throwable ex) {
                    // StorageVolume.getDirectory() does not exist in Android 10 and earlier.
                    // disable the feature in that case
                    return null;
                }
            }
        }
        catch(Throwable ex){
            //ex.printStackTrace();
        }
        return null;
    }

    public long getAvailableSpace(File dir) {
        // Fetching internal memory information

        try {
             if ((dir != null) && dir.exists()) {
                StatFs stat = new StatFs(dir.getPath());
                long blockSize = stat.getBlockSizeLong();
                long availableBlocks = stat.getAvailableBlocksLong();
                long totalBlocks = stat.getBlockCountLong();
                long availableSpace = availableBlocks * blockSize;
                long totalSpace = totalBlocks * blockSize;
                return availableSpace;
            }
        }
        catch(Throwable ex){
            //ex.printStackTrace();
        }
        return -1;
    }

    private String getSizeMessage(long mbSize)  {
        if (mbSize < 1) {
            return getString(R.string.space_less_than_one_meg);
        }
        if (mbSize > 1000) {
            long gbSize = mbSize / 1000;
            mbSize = mbSize - (gbSize * 1000);
            mbSize = mbSize / 10;
            return gbSize + "." + mbSize + getString(R.string.space_gb);
        }
        return "" + mbSize + getString(R.string.space_mb);
    }

    private void doDownload(String urlList[]) {
        setContentView(R.layout.activity_download);

        pText = (TextView) findViewById(R.id.pProgressText);
        pBar = (ProgressBar) findViewById(R.id.progressBar);
        at.execute(urlList);
    }

    public boolean folderExists(Context con, String pURL) {
        String fName = pURL.substring(pURL.lastIndexOf("/") + 1);
        String fileName;
        try {
            fileName = java.net.URLDecoder.decode(fName, StandardCharsets.UTF_8.name());
            String folderName = at.removeExtension(fileName);
            // If bloom file has not already been parsed, download it and parse it
            return org.tyndalebt.storyproduceradv.tools.file.FileIO.workspaceRelPathExists(con, folderName);
        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
            return true;  // This won't do anything (Download file or show in list)
        }
    }

    public String[] BuildURLList() {
        String pURLs = "";
        Integer idx;

        for (idx = 0; idx < pView.getCount(); idx++) {
            Object obj = pView.getAdapter().getItem(idx);
            DownloadDS dataModel=(DownloadDS) obj;
            if (dataModel.getChecked() == true) {
                if (pURLs != "") {
                    pURLs = pURLs + "|";
                }
                pURLs = pURLs + dataModel.getURL();
            }
        }
        return (pURLs == "") ? null : pURLs.split("\\|");
    }

    public String URLEncodeUTF8(String pSource) {
        String tmpNew = "";
        Integer tmpInt = 0;
        char tmpByte = 0;
        Integer idx;

        for (idx = 0; idx < pSource.length(); idx++) {
            if (pSource.charAt(idx) >= 128) {
                tmpByte = pSource.charAt(idx);
                tmpNew = tmpNew + "%";
                tmpInt = (int)tmpByte;
                tmpNew = tmpNew + String.format("%02X", tmpInt);
            } else if (pSource.charAt(idx) == ' ') {
                tmpNew = tmpNew + "%20";
            } else if (pSource.charAt(idx) == '#') {
                    tmpNew = tmpNew + "%23";
            } else {
                tmpNew = tmpNew + pSource.charAt(idx);
            }
        }
        return tmpNew;
    }

    public void setChosenLanguage(String pNativeLanguage) {
        String DisplayLine[] = pNativeLanguage.split("/");
        this.chosenLanguage = getEnglishLangName(DisplayLine[0].trim());
    }

    public boolean copyFile(String outFile) {
        int i;
        String result = "";

        if (outFile.compareTo(BLOOM_LIST_FILE) == 0) {
            if (firstPass == true) {
                InputStream fis = null;
                try {
                    File sourceFile = new File(this.getFilesDir() + "/" + outFile);
                    fis = new FileInputStream(sourceFile);
                    char current;
                    while (fis.available() > 0) {
                        current = (char) fis.read();
                        result = result + String.valueOf(current);
                    }
                } catch (Exception e) {

                    Log.d("DownloadActivity:copyFile", e.toString());
                    Intent mDisplayAlert = new Intent(this, DisplayAlert.class);
                    mDisplayAlert.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mDisplayAlert.putExtra("title", getString(R.string.more_templates));
                    mDisplayAlert.putExtra("body", getString(R.string.remote_check_msg_no_connection));
                    startActivity(mDisplayAlert);
                }
                bloomFileContents = result;
            } else {
                result = bloomFileContents;
            }

            String lines[] = result.split("\\r?\\n");
            String itemString = "";
            String tagString = "";
            int idx;
            String lastLang = "";

            for (idx = 0; idx < lines.length; idx++) {
                String lang[] = lines[idx].split("/");
                if (firstPass == true) {
                    if (!lastLang.equals(lang[0])) {
                        if (!itemString.equals("")) {
                            itemString = itemString + "|";
                            tagString = tagString + "|";
                        }
                        lastLang = lang[0];
                        itemString = itemString + lang[0];
                        tagString = tagString + "Language";
                    }
                } else {
                    if (lang[0].equals(this.chosenLanguage)) {
                        if (!itemString.equals("")) {
                            itemString = itemString + "|";
                            tagString = tagString + "|";
                        }
                        if (lang.length > 1) {
                            ByteBuffer buffer = StandardCharsets.ISO_8859_1.encode(lang[1]);
                            String encodedString = StandardCharsets.UTF_8.decode(buffer).toString();
                            itemString = itemString + encodedString;
                        }
                        tagString = tagString + file_url + URLEncodeUTF8(lines[idx]);
                    }
                }
            }
            String itemArray[] = itemString.split("\\|");
            String tagArray[] = tagString.split("\\|");
            at = new DownloadFileFromURL(this);
            buildBloomList(itemArray, tagArray);
            firstPass = false;
        } else {
            BaseController upstor = new BaseController(this, this);
            pBar.setVisibility(View.INVISIBLE);
            pText.setVisibility(View.INVISIBLE);
            Workspace.parseLanguage = this.chosenLanguage;
            File sourceFile = new File(this.getFilesDir() + "/" + BLOOM_LIST_FILE);
            sourceFile.delete();
            //  All bloom files have been download and Bloomlist file deleted.  Wait for a second, then build list of bloom files and parse them
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // e.printStackTrace();
            }
            // Build list of (downloaded) bloom files on internal folder
            ArrayList<DocumentFile> storyFiles=new ArrayList<>();
            DocumentFile root = DocumentFile.fromFile(new File(this.getFilesDir() + "/"));
            for(DocumentFile f:root.listFiles()){
                 if(f.isFile()){
                    String name=f.getName();
                    if(name.endsWith(".bloomSource")
                            || name.endsWith(".bloom"))
                        storyFiles.add(f);
                }
            }
            upstor.updateStoriesCommon(storyFiles);
        }
        return true;
    }

    private Boolean NavItemSelected(MenuItem menuItem) {
        mDrawerLayout.closeDrawers();

        switch (menuItem.getItemId()) {
            case R.id.nav_workspace:
                showSelectTemplatesFolderDialog();
                break;
            case R.id.nav_word_link_list:
                showWordLinksList();
                break;
            case R.id.nav_more_templates:
                // current fragment
                break;
            case R.id.nav_stories:
                showMain();
                break;
            case R.id.nav_registration:
                showRegistration(false);
                break;
            case R.id.change_language:
                showChooseLanguage();
                break;
            case R.id.video_share:
                showVideos();
                break;
            case R.id.backup_restore:
                showBackupRestore();
                break;
            case R.id.nav_spadv_website:
                org.tyndalebt.storyproduceradv.tools.file.FileIO.goToURL(this, Workspace.URL_FOR_WEBSITE);
                break;
            case R.id.nav_about:
                showAboutDialog();
                break;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.helpButton:
/*
                WebView wv = WebView(this);
                val iStream = assets.open(Phase.getHelpDocFile(PhaseType.STORY_LIST))
                val text = iStream.reader().use {
                it.readText() }

            wv.loadDataWithBaseURL(null,text,"text/html",null,null)
            val dialog = AlertDialog.Builder(this)
                    .setTitle("Story List Help")
                    .setView(wv)
                    .setNegativeButton("Close") { dialog, _ ->
                    dialog!!.dismiss()
            }
            dialog.show()
 */
                break;
            default:
                super.onOptionsItemSelected(item);
                break;
        }
        return true;

    }

}
