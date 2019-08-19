package com.sgo.saldomu.activities

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v7.app.AlertDialog
import android.view.MenuItem
import com.sgo.saldomu.R
import com.sgo.saldomu.adapter.FavoriteAdapter
import com.sgo.saldomu.fragments.FavoriteFragment
import com.sgo.saldomu.widgets.BaseActivity
import kotlinx.android.synthetic.main.activity_favorite.*
import java.util.*

private const val TAG = "FavoriteActivity"

class FavoriteActivity : BaseActivity() {
    internal lateinit var dialog: AlertDialog
    internal lateinit var adapter: FavoritePagerAdapter

    override fun getLayoutResource(): Int {
        return R.layout.activity_favorite
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize()
    }

    private fun initialize() {
        setActionBarIcon(R.drawable.ic_arrow_left)
        actionBarTitle = getString(R.string.menu_item_favorite)

        val builder = AlertDialog.Builder(this)
        builder.setView(R.layout.progress)
        dialog = builder.create()


        // Get the ViewPager and set it's PagerAdapter so that it can display items
        adapter = FavoritePagerAdapter(supportFragmentManager, this)
//        viewPager.setPageMargin(dpToPx(10))
        viewPager.adapter = adapter

        // Give the TabLayout the ViewPager
        tab_layout.setupWithViewPager(viewPager)
    }

    private fun setDialog(show: Boolean) {
        if (show)
            dialog.show()
        else
            dialog.dismiss()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class FavoritePagerAdapter(fm: FragmentManager, private val context: Context) : FragmentStatePagerAdapter(fm) {
        private val PAGE_COUNT = 3
        private val tabTitles = arrayOf("BIL", "BBS", "TRF")
        private val bilFragment: FavoriteFragment = FavoriteFragment().newInstance("BIL")
        private val bbsFragment: FavoriteFragment = FavoriteFragment().newInstance("BBS")
        private val trfFragment: FavoriteFragment = FavoriteFragment().newInstance("TRF")


        override fun getCount(): Int {
            return PAGE_COUNT
        }

        override fun getItem(position: Int): Fragment = when (position) {
            0 -> bilFragment
            1 -> bbsFragment
            else -> trfFragment
        }

        override fun getPageTitle(position: Int): CharSequence {
            // Generate title based on item position
            return tabTitles[position]
        }
    }
}