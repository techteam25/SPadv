package org.tyndalebt.storyproduceradv.controller.export

import android.app.AlertDialog
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.widget.CheckBox
import androidx.documentfile.provider.DocumentFile
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.view.BaseActivityView
import org.tyndalebt.storyproduceradv.controller.BaseController
import org.tyndalebt.storyproduceradv.model.Story
import org.tyndalebt.storyproduceradv.model.VIDEO_DIR
import org.tyndalebt.storyproduceradv.tools.file.copyToFilesDir
import org.tyndalebt.storyproduceradv.tools.file.getFileType
import java.io.File
import java.io.OutputStream


class SelectCopyFolderController(
        view: BaseActivityView,
        context: Context,
        val workspace: Workspace
) : BaseController(view, context) {

    var mHelper : VideoListHelper? = null

    fun openDocumentTree(request: Int, helper : VideoListHelper) {
        mHelper = helper
        val selectedVideos = mHelper!!.getSelectedVideos()
        if (selectedVideos!!.size > 0) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(URI_PERMISSION_FLAGS)
            view.startActivityForResult(intent, request)
        }
    }

    fun onFolderSelected(request: Int, result: Int, data: Intent?) {
        data?.data?.also { uri ->
            if (result == Activity.RESULT_OK) {
                view.takePersistableUriPermission(uri)
                Workspace.setupVideoCopyPath(mHelper!!.mActivity!!, uri)
                mHelper!!.checkForExistingFilesAndCopy(Workspace.videoCopyPath.uri)
            }
        }
    }

     companion object {

        const val SELECT_COPY_FOLDER = 60
        //const val UPDATE_TEMPLATES_FOLDER = 53
        val SELECT_COPY_FOLDER_REQUEST_CODES = arrayOf(
        //        SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO,
        //        UPDATE_TEMPLATES_FOLDER,
                SELECT_COPY_FOLDER
        )

        const val URI_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }

}