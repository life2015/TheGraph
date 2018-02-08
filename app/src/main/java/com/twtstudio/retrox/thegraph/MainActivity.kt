package com.twtstudio.retrox.thegraph

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat
import android.util.Log
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val typeFace = ResourcesCompat.getFont(this, R.font.custom_regular)

        val count = container.childCount
        (0 until count).forEach {
            val view = container.getChildAt(it)
            if (view is TextView) {
                Log.d("ChangeTypeFace", view.text.toString())
                view.setTypeface(typeFace)
            }
        }

//        fitSystemWindowWithStatusBar(container)
//        fitSystemWindowWithNavigationBar(container)
    }
}
