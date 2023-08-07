package org.tyndalebt.storyproduceradv.controller.storylist

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import org.tyndalebt.storyproduceradv.model.Workspace
import org.tyndalebt.storyproduceradv.view.BaseActivityView
import org.tyndalebt.storyproduceradv.controller.BaseController



class SelectBackupFolderController(
        view: BaseActivityView,
        context: Context,
        val workspace: Workspace
) : BaseController(view, context) {

    var mFrag : BackupStoryPageFragment? = null

    // TODO - can I specify the directory that is initially displayed?
    fun openDocumentTree(request: Int, frag : BackupStoryPageFragment, initUri : Uri?) {
        mFrag = frag
        val activity = mFrag!!.activity as BackupRestoreActivity
        activity.backupController = this
        var flags = URI_PERMISSION_FLAGS
        if (request == SELECT_RESTORE_FOLDER) {
           flags = URI_READ_PERMISSION_FLAGS
        }

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(flags)
        if (initUri != null) {
           intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initUri)
        }
        view.startActivityForResult(intent, request)
    }

    fun onFolderSelected(request: Int, result: Int, data: Intent?) {
        data?.data?.also { uri ->
            if (result == Activity.RESULT_OK) {
               if (request == SELECT_BACKUP_FOLDER) {
                  view.takePersistableUriPermission(uri)
                  Workspace.setupStoryBackupPath(mFrag!!.context!!, uri)
                  mFrag!!.checkForExistingBackupFilesAndCopy()
               }
               else if (request == SELECT_RESTORE_FOLDER) {

                  val docFile = DocumentFile.fromTreeUri(context, uri)!!
                  mFrag!!.restoreStory(docFile.uri)
               }
            }
        }
    }

     companion object {

        const val SELECT_BACKUP_FOLDER = 61
        const val SELECT_RESTORE_FOLDER = 62
        //const val UPDATE_TEMPLATES_FOLDER = 53
        val SELECT_BACKUP_FOLDER_REQUEST_CODES = arrayOf(
        //        SELECT_TEMPLATES_FOLDER_AND_ADD_DEMO,
        //        UPDATE_TEMPLATES_FOLDER,
                SELECT_BACKUP_FOLDER,
                SELECT_RESTORE_FOLDER
        )

        const val URI_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION

        const val URI_READ_PERMISSION_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
    }

}
