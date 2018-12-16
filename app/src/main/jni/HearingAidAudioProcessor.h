//
// Created by frede on 10.01.2018.
//

#ifndef HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
#define HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H


#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <SuperpoweredNBandEQ.h>
#include <cmath>
#include <android/log.h>

class HearingAidAudioProcessor {
public:
    HearingAidAudioProcessor(unsigned int samplerate, unsigned int buffersize, float *frequencies);

    ~HearingAidAudioProcessor() {
        delete audioSystem;
        delete superpoweredEq;
    }

    bool process(short int *output, unsigned int numberOfSamples);

    void onPlayPause(bool play);

    void enableEQ(bool eqEnabled) {
        this->eqEnabled = eqEnabled;
        __android_log_print(ANDROID_LOG_VERBOSE, "HearingAid", "!!!!! Before enable()... !!!!!");
        get_superpowered_eq()->enable(eqEnabled);
        __android_log_print(ANDROID_LOG_INFO, "HearingAid", "!!!!! After enable()... !!!!!");
    }

    inline SuperpoweredNBandEQ *get_superpowered_eq() {
        return superpoweredEq;
    }

    /*
 @brief Call this in the main activity's onResume() method.

  Calling this is important if you'd like to save battery. When there is no audio playing and the app goes to the background, it will automatically stop audio input and/or output.
*/
    void onForeground();

/*
 @brief Call this in the main activity's onPause() method.

 Calling this is important if you'd like to save battery. When there is no audio playing and the app goes to the background, it will automatically stop audio input and/or output.
*/
    void onBackground();

/*
 @brief Starts audio input and/or output.
*/
    void start();

/*
 @brief Stops audio input and/or output.
*/
    void stop();

private:
    SuperpoweredAndroidAudioIO *audioSystem;
    SuperpoweredNBandEQ *superpoweredEq;
    bool eqEnabled;
};


#endif //HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
