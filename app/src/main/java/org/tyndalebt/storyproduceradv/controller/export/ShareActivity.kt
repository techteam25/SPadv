package org.tyndalebt.storyproduceradv.controller.export

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.controller.phase.PhaseBaseActivity
import org.tyndalebt.storyproduceradv.model.Workspace


/**
 * Created by annmcostantino on 10/1/2017.
 */

class ShareActivity : PhaseBaseActivity() {

    private val mHelper = VideoListHelper()

     /**
     * Returns the the video paths that are saved in preferences and then checks to see that they actually are files that exist
     * @return Array list of video paths
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share)
        invalidateOptionsMenu()
        if (Workspace.activeStory.isApproved) {
            findViewById<View>(R.id.lock_overlay).visibility = View.INVISIBLE
        } else {
            val mainLayout = findViewById<View>(R.id.main_linear_layout)
            PhaseBaseActivity.disableViewAndChildren(mainLayout)
        }
        mHelper.initView(this, Workspace.activeStory)
        runOnUiThread{
            //This allows the video file to write if it just did
            val handler = Handler()
            handler.postDelayed({
                mHelper!!.refreshViews()
                //your code here
            }, 3000)
        }
    }

    override fun onActivityResult(request: Int, result: Int, data: Intent?) {
        super.onActivityResult(request, result, data)
        mHelper.checkActivityResult(request, result, data)
    }

    fun getHelper() : VideoListHelper {  // used by the tests
        return mHelper
    }
}
