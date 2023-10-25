package com.obrekht.neowork.users.ui.profile

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.obrekht.neowork.R
import com.obrekht.neowork.users.ui.profile.jobs.JobsFragment
import com.obrekht.neowork.users.ui.profile.wall.WallFragment

class UserProfileTabsAdapter(
    fragment: Fragment,
    private val userId: Long
) : FragmentStateAdapter(fragment) {

    enum class Tabs(@StringRes val titleResId: Int) {
        WALL(R.string.tab_wall),
        JOBS(R.string.tab_jobs)
    }

    fun getTab(position: Int) = Tabs.values()[position]

    override fun getItemCount(): Int = Tabs.values().size

    override fun createFragment(position: Int): Fragment =
        when (Tabs.values()[position]) {
            Tabs.WALL -> WallFragment.newInstance(userId)
            Tabs.JOBS -> JobsFragment.newInstance(userId)
        }
}