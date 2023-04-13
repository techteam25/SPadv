package org.tyndalebt.storyproduceradv.controller.pager

import android.widget.Toast
import org.tyndalebt.storyproduceradv.App
import org.tyndalebt.storyproduceradv.model.Workspace

/**
 * Class that implements the ViewPager.OnPageChangeListener to give the view pager circular functionality
 */
class CircularViewPagerHandler(private val mViewPager: androidx.viewpager.widget.ViewPager) : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
    private var mScrollState: Int = 0

    override fun onPageSelected(position: Int) {
        /**
         * Will handle this with a Gesture listener on the appropriate phases
         *
        */

        Workspace.activeSlideNum = position
//        Workspace.slideNumRightOfActive = position + 1;
//        Workspace.slideNumLeftOfActive = position - 1;

    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE && mScrollState != androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING) {
            handleSetNextItem()
        }
        mScrollState = state
    }

    private fun handleSetNextItem() {
        val lastPosition = mViewPager.adapter!!.count - 1
        if (Workspace.activeSlideNum == 0) {
            mViewPager.setCurrentItem(lastPosition, false)
        } else if (Workspace.activeSlideNum == lastPosition) {
            mViewPager.setCurrentItem(0, false)
        }
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }
}
