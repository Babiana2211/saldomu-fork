package com.sgo.saldomu.activities;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.widget.Button;

import com.github.paolorotolo.appintro.AppIntro;
import com.securepreferences.SecurePreferences;
import com.sgo.saldomu.R;
import com.sgo.saldomu.coreclass.CustomSecurePref;
import com.sgo.saldomu.coreclass.DefineValue;
import com.sgo.saldomu.fragments.IntroPage;

/**
 * Created by Lenovo Thinkpad on 7/20/2017.
 */

public class TutorialActivity extends AppIntro {
    private int intType;
    public static final int tutorial_payFriend=1;
    public static final int tutorial_askMoney=2;
    public static final int tutorial_topUp=3;
    public static final int tutorial_belanja=4;
    public static final int tutorial_report=5;
    public static final int tutorial_bbs=6;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);

    }

    @Override
    public void init(@Nullable Bundle savedInstanceState) {
        intType = getIntent().getIntExtra(DefineValue.TYPE,0);
        if(intType==0)
        {
            this.finish();
        }
        else if (intType==tutorial_payFriend)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }
        else if (intType==tutorial_askMoney)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }
        else if (intType==tutorial_topUp)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }
        else if (intType==tutorial_belanja)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }
        else if (intType==tutorial_report)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }else if (intType==tutorial_bbs)
        {
            addSlide(IntroPage.newInstance(R.layout.intro1_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro2_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro3_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro4_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro5_fragment));
            addSlide(IntroPage.newInstance(R.layout.intro6_fragment));

            setFlowAnimation();
            Button skipbtn = (Button)skipButton;
            Button donebtn = (Button)doneButton;
            skipbtn.setText(getString(R.string.start_now));
            donebtn.setText(getString(R.string.done));
        }
    }

    @Override
    public void onSkipPressed() {
        show();
    }

    @Override
    public void onNextPressed() {

    }

    @Override
    public void onDonePressed() {
        show();
    }

    @Override
    public void onSlideChanged() {

    }
    private void show(){
        SecurePreferences sp = CustomSecurePref.getInstance().getmSecurePrefs();
        SecurePreferences.Editor mEditor=sp.edit();

        if (intType==tutorial_payFriend)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_PAY_FRIEND,false);
        }
        else if (intType==tutorial_askMoney)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_ASK_MONEY,false);
        }
        else if (intType==tutorial_belanja)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_BELANJA,false);
        }
        else if (intType==tutorial_report)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_REPORT,false);
        }
        else if (intType==tutorial_topUp)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_TOP_UP,false);
        }
        else if (intType==tutorial_bbs)
        {
            mEditor.putBoolean(DefineValue.TUTORIAL_BBS,false);
        }
        mEditor.apply();
        finish();
    }
}
