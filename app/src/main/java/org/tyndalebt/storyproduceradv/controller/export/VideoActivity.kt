package org.tyndalebt.storyproduceradv.controller.export

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.MainBaseActivity
import org.tyndalebt.storyproduceradv.controller.SelectTemplatesFolderController
import org.tyndalebt.storyproduceradv.model.Phase
import org.tyndalebt.storyproduceradv.model.PhaseType
import org.tyndalebt.storyproduceradv.model.Workspace
import java.io.InputStream


class VideoActivity : MainBaseActivity()  {

    private val mHelper = VideoListHelper()

    /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videos_shell)
        doSetContentView(R.layout.activity_videos)
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
    }


    // If this menu item is selected, do nothing
    // since this is the currently selected page.
    override fun getMenuItemId() : Int {
      return R.id.video_share
    }

    override fun getTitleString() : String? {
        return getString(R.string.video_share)
    }

    override fun getTitleColor2() : Int {
        return R.color.darkGray
    }

    override fun openHelpFile() : InputStream {
        return  Phase.openHelpDocFile(PhaseType.COPY_VIDEOS, Workspace.activeStory.language,this)
    }

    //Override setContentView to coerce into child view.
    fun doSetContentView(id: Int) {
        val layout : LinearLayout = findViewById(R.id.linear_layout)
        val inflater = layoutInflater
        inflater.inflate(id, layout)
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        mHelper.checkActivityResult(request, result, data)
    }

    fun getHelper() : VideoListHelper {  // used by the tests
        return mHelper
    }
}
