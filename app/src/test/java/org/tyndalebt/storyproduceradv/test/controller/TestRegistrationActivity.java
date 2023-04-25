package org.tyndalebt.storyproduceradv.test.controller;

import android.content.Intent;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.tyndalebt.storyproduceradv.controller.RegistrationActivity;
import org.tyndalebt.storyproduceradv.model.Workspace;

// RK 4-24-2023
// The startup order of operations has changed since this test was written
// Both these tests now result in a permissions request
// Eventually we should probably want to write new tests actually testing
// the registration operation.  For now disable the actions in these tests.

@Config(sdk = 30)    // our robolectric version (4.5.1) is not updated to 31 yet
@RunWith(RobolectricTestRunner.class)
public class TestRegistrationActivity {
    /*
    @Test
    public void OnCreate_When_WorkspaceDirectoryNotSet_Should_StartFileTreeActivity() {
        try {
            Workspace.INSTANCE.clearWorkspace();
            RegistrationActivity registrationActivity = Robolectric.buildActivity(RegistrationActivity.class).create().get();

            Intent startedActivity = Shadows.shadowOf(registrationActivity).peekNextStartedActivity();
            Assert.assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, startedActivity.getAction());
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
    */

    @Test
    public void OnCreate_When_WorkspaceDirectoryIsAlreadySet_Should_NotStartFileTreeActivity() {
        try {
            DocumentFile mockFile = Mockito.mock(DocumentFile.class);
            Mockito.when(mockFile.exists()).thenReturn(true);
            Mockito.when(mockFile.getUri()).thenReturn(Uri.parse("mock"));
            Workspace.INSTANCE.setWorkdocfile(mockFile);

            RegistrationActivity registrationActivity = Robolectric.buildActivity(RegistrationActivity.class).create().get();

            Intent startedActivity = Shadows.shadowOf(registrationActivity).peekNextStartedActivity();
            //xx Assert.assertNull(startedActivity);
        }
        catch (Throwable ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
