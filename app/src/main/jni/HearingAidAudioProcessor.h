//
// Created by frede on 10.01.2018.
//

#ifndef HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
#define HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H


#include <OpenSource/SuperpoweredAndroidAudioIO.h>

class HearingAidAudioProcessor {
public:
    HearingAidAudioProcessor(unsigned int samplerate, unsigned int buffersize);
    ~HearingAidAudioProcessor();

    bool process(short int *output, unsigned int numberOfSamples);
    void onPlayPause(bool play);
    void enableEQ(bool eqEnabled);

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
    bool eqEnabled;
};


#endif //HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
