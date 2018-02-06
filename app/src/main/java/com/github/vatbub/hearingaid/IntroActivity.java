package com.github.vatbub.hearingaid;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.github.paolorotolo.appintro.model.SliderPage;

/**
 * Created by frede on 15.01.2018.
 */

public class IntroActivity extends AppIntro {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        /*addSlide(firstFragment);
        addSlide(secondFragment);
        addSlide(thirdFragment);
        addSlide(fourthFragment);*/

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        SliderPage sliderPage1 = new SliderPage();
        sliderPage1.setTitle(getString(R.string.intro_slide_1_title));
        sliderPage1.setDescription(getString(R.string.intro_slide_1_description));
        sliderPage1.setImageDrawable(R.drawable.welcomeribbon);

        SliderPage sliderPage2 = new SliderPage();
        sliderPage2.setTitle(getString(R.string.intro_slide_2_title));
        sliderPage2.setDescription(getString(R.string.intro_slide_2_description));
        sliderPage2.setImageDrawable(R.drawable.smartphonewithmic);

        SliderPage sliderPage3 = new SliderPage();
        sliderPage3.setTitle(getString(R.string.intro_slide_2_title));
        sliderPage3.setDescription(getString(R.string.intro_slide_3_description));
        sliderPage3.setImageDrawable(R.drawable.headphones);

        SliderPage sliderPage4 = new SliderPage();
        sliderPage4.setTitle(getString(R.string.intro_slide_4_title));
        sliderPage4.setDescription(getString(R.string.intro_slide_4_description));
        sliderPage4.setImageDrawable(R.drawable.thumbup);

        addSlide(AppIntroFragment.newInstance(sliderPage1));
        addSlide(AppIntroFragment.newInstance(sliderPage2));
        addSlide(AppIntroFragment.newInstance(sliderPage3));
        addSlide(AppIntroFragment.newInstance(sliderPage4));

        askForPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 4);

        // OPTIONAL METHODS
        // Override bar/separator color.
        // setBarColor(Color.parseColor("#3F51B5"));
        // setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done button.
        // showSkipButton(false);
        // setProgressButtonEnabled(false);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        // setVibrate(true);
        // setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        onDonePressed(currentFragment);
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }
}
