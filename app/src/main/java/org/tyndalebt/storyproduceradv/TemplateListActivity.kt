package org.tyndalebt.storyproduceradv

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.widget.LinearLayout
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.controller.export.TemplateListHelper
import org.tyndalebt.storyproduceradv.model.*
import java.io.File
import java.io.InputStream

class TemplateListActivity : MainBaseActivity() {
    var fileIndex: Int = 0
    private val mHelper = TemplateListHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_template_shell)
        doSetContentView(R.layout.activity_template_list)
        setupDrawer()
        initActionBar()
        invalidateOptionsMenu()
        // findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE  // turn off lock icon

        mHelper.initView(this, null)
        runOnUiThread{
            //This allows the video file to write if it just did
            val handler = Handler()
            handler.postDelayed({
                mHelper!!.refreshViews()
                //your code here
            }, 3000)
        }
        checkForUploads()
    }

    //Override setContentView to coerce into child view.
    private fun doSetContentView(id: Int) {
        val layout : LinearLayout = findViewById(R.id.linear_layout)
        val inflater = layoutInflater
        inflater.inflate(id, layout)
    }

    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId() : Int {
        return R.id.templates_created
    }

    override fun getTitleString() : String? {
        return getString(R.string.templates_created)
    }

    override fun openHelpFile() : InputStream {
        return  Phase.openHelpDocFile(PhaseType.LIST_TEMPLATES, Workspace.activeStory.language,this)
    }

    private fun checkForUploads() {
        // Get list of zip files
        var zipList: ArrayList <String> = ArrayList()

        try {
            val srcZipFolder = "$NEW_TEMPLATES_DIR/"
            val zipFolder = getAbsolutePathFromDocumentUri(this, Workspace.workdocfile.uri) + srcZipFolder
            val dir = File(zipFolder)
            if (dir.exists() && dir.isDirectory) {
                val list = dir.listFiles()
                if (list != null) {
                    for (file in list) {
                        val templateZip = "$file.zip"
                        val zipFile = File(templateZip)
                        if (zipFile.exists()) {
                            zipList.add(file.name.replace(".zip", ""))
                        }
                    }
                }
            }
        }
        catch (e: Exception)  {}
        fileIndex = 0
        doOne(getNextName(zipList), zipList)
    }

    private fun getNextName(pList: ArrayList <String>) : String {
        var destStoryName: String = ""
        if (pList.size <= fileIndex) {
            return "" // Done
        }
        destStoryName = pList[fileIndex]
        return destStoryName
    }

    private fun doOne(pDestStoryName: String, pList: ArrayList <String>) {
        if (pDestStoryName == "") {
            return
        }
        if (Build.VERSION.SDK_INT > 8) {
            val policy = StrictMode.ThreadPolicy.Builder()
                    .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            msgDialog = AlertDialog.Builder(this)
                    .setTitle(R.string.upload_server)
                    .setMessage(pDestStoryName + "\n\n" + this.getString(R.string.template_upload_wait))
                    .setPositiveButton(this.getString(R.string.ok)) { _, _ ->
                        if (goForIt(pDestStoryName)) {
                            fileIndex += 1
                            doOne(getNextName(pList), pList)
                        } else {
                            fileIndex = 9999 // fake done
                        }
                    }
                    .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                        fileIndex = 9999 // fake done
                    }
                    .create()
            msgDialog.show()
        }
    }
}
