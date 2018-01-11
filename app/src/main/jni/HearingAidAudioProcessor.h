//
// Created by frede on 10.01.2018.
//

#ifndef HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
#define HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H


#include <AndroidIO/SuperpoweredAndroidAudioIO.h>

class HearingAidAudioProcessor {
public:
    HearingAidAudioProcessor(unsigned int samplerate, unsigned int buffersize);
    ~HearingAidAudioProcessor();

    bool process(short int *output, unsigned int numberOfSamples);
    void onPlayPause(bool play);

private:
    SuperpoweredAndroidAudioIO *audioSystem;
    bool silence;
};


#endif //HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
