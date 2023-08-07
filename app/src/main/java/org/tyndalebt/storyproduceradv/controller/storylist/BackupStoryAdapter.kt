package org.tyndalebt.storyproduceradv.controller.storylist

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.tyndalebt.storyproduceradv.R
import org.tyndalebt.storyproduceradv.activities.AppCompatActivityMTT

class BackupStoryAdapter(activity: AppCompatActivityMTT, private val itemsCount: Int) :
        StoryPageAdapter(activity, itemsCount) {

    override fun createFragment(position: Int): Fragment {
        return BackupStoryPageFragment.getInstance(position)
    }
}
