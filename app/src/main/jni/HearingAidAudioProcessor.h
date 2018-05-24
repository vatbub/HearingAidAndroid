//
// Created by frede on 10.01.2018.
//

#ifndef HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
#define HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H


#include <AndroidIO/SuperpoweredAndroidAudioIO.h>
#include <cmath>

class HearingAidAudioProcessor {
public:
    HearingAidAudioProcessor(unsigned int samplerate, unsigned int buffersize);

    ~HearingAidAudioProcessor() {
        delete audioSystem;
    }

    bool process(short int *output, unsigned int numberOfSamples);

    void onPlayPause(bool play);

    void enableEQ(bool eqEnabled) {
        this->eqEnabled = eqEnabled;
    }

    void setEQ(float *eq, jsize eqSize) {
        this->eq = eq;
    }

    void set_min_frequency(float min_frequency) {
        this->min_frequency = min_frequency;
    }

    void set_max_frequency(float max_frequency) {
        this->max_frequency = max_frequency;
    }

    float get_eq_index_for_frequency(float *magnitudeBegin, float *currentMagnitude) {
        float currentFrequency = bucket_size * (currentMagnitude - magnitudeBegin) / sizeof(float);

        float min_frequency_delta = 100000; // will never be reached, can be considered the max value
        int min_frequency_delta_eq_index = 0; // will never be reached, can be considered the max value
        for (int i = 0; i < eqSize; i++) {
            float frequency_delta = std::abs(get_frequency_of_eq_bin(i) - currentFrequency);
            if (frequency_delta < min_frequency_delta) {
                min_frequency_delta = frequency_delta;
                min_frequency_delta_eq_index = i;
            }
        }

        return min_frequency_delta_eq_index;
    }

    float get_frequency_of_eq_bin(int eq_index) {
        return eq_index * (max_frequency - min_frequency) / eqSize;
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
    bool eqEnabled;
    float *eq;
    jsize eqSize;
    float bucket_size = 21.5; // Hz per float
    float min_frequency;
    float max_frequency;
};


#endif //HEARINGAID_HEARINGAIDAUDIOPROCESSOR_H
