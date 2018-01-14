//
// Created by frede on 10.01.2018.
//

#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <jni.h>
#include <SuperpoweredSimple.h>
#include <string.h>
#include "HearingAidAudioProcessor.h"
#include <SuperpoweredFrequencyDomain.h>
#include <malloc.h>
#include <SuperpoweredCPU.h>

static HearingAidAudioProcessor *jniInstance = NULL;

static SuperpoweredFrequencyDomain *frequencyDomain;
static float *magnitudeLeft, *magnitudeRight, *phaseLeft, *phaseRight, *fifoOutput, *inputBufferFloat;
static int fifoOutputFirstSample, fifoOutputLastSample, stepSize, fifoCapacity;

#define FFT_LOG_SIZE 11 // 2^11 = 2048

static bool
audioProcessing(void *clientdata, short int *audioIO, int numberOfSamples, int samplerate) {
    return ((HearingAidAudioProcessor *) clientdata)->process(audioIO,
                                                              (unsigned int) numberOfSamples);
}

HearingAidAudioProcessor::HearingAidAudioProcessor(unsigned int samplerate,
                                                   unsigned int buffersize) {
    frequencyDomain = new SuperpoweredFrequencyDomain(FFT_LOG_SIZE); // This will do the main "magic".
    stepSize = frequencyDomain->fftSize / 4; // The default overlap ratio is 4:1, so we will receive this amount of samples from the frequency domain in one step.

    // Frequency domain data goes into these buffers:
    magnitudeLeft = (float *)malloc(frequencyDomain->fftSize * sizeof(float));
    magnitudeRight = (float *)malloc(frequencyDomain->fftSize * sizeof(float));
    phaseLeft = (float *)malloc(frequencyDomain->fftSize * sizeof(float));
    phaseRight = (float *)malloc(frequencyDomain->fftSize * sizeof(float));

    // Time domain result goes into a FIFO (first-in, first-out) buffer
    fifoOutputFirstSample = fifoOutputLastSample = 0;
    fifoCapacity = stepSize * 100; // Let's make the fifo's size 100 times more than the step size, so we save memory bandwidth.
    fifoOutput = (float *)malloc(fifoCapacity * sizeof(float) * 2 + 128);

    inputBufferFloat = (float *)malloc(buffersize * sizeof(float) * 2 + 128);

    SuperpoweredCPU::setSustainedPerformanceMode(true);

    audioSystem = new SuperpoweredAndroidAudioIO(samplerate, buffersize, true, true,
                                                 audioProcessing, this,
            // SL_ANDROID_RECORDING_PRESET_GENERIC,
                                                 SL_ANDROID_RECORDING_PRESET_UNPROCESSED,
            // SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION,
                                                 SL_ANDROID_STREAM_MEDIA, 0);
}

HearingAidAudioProcessor::~HearingAidAudioProcessor() {
    delete audioSystem;
}

bool HearingAidAudioProcessor::process(short int *output, unsigned int numberOfSamples) {
    SuperpoweredShortIntToFloat(output, inputBufferFloat,
                                numberOfSamples); // Converting the 16-bit integer samples to 32-bit floating point.
    frequencyDomain->addInput(inputBufferFloat,
                              numberOfSamples); // Input goes to the frequency domain.

    // In the frequency domain we are working with 1024 magnitudes and phases for every channel (left, right), if the fft size is 2048.
    while (frequencyDomain->timeDomainToFrequencyDomain(magnitudeLeft, magnitudeRight, phaseLeft,
                                                        phaseRight)) {
        // You can work with frequency domain data from this point.

        // This is just a quick example: we remove the magnitude of the first 20 bins, meaning total bass cut between 0-430 Hz.
        memset(magnitudeLeft, 0, 80);
        memset(magnitudeRight, 0, 80);

        // We are done working with frequency domain data. Let's go back to the time domain.

        // Check if we have enough room in the fifo buffer for the output. If not, move the existing audio data back to the buffer's beginning.
        if (fifoOutputLastSample + stepSize >=
            fifoCapacity) { // This will be true for every 100th iteration only, so we save precious memory bandwidth.
            int samplesInFifo = fifoOutputLastSample - fifoOutputFirstSample;
            if (samplesInFifo > 0)
                memmove(fifoOutput, fifoOutput + fifoOutputFirstSample * 2,
                        samplesInFifo * sizeof(float) * 2);
            fifoOutputFirstSample = 0;
            fifoOutputLastSample = samplesInFifo;
        };

        // Transforming back to the time domain.
        frequencyDomain->frequencyDomainToTimeDomain(magnitudeLeft, magnitudeRight, phaseLeft,
                                                     phaseRight,
                                                     fifoOutput + fifoOutputLastSample * 2);
        frequencyDomain->advance();
        fifoOutputLastSample += stepSize;
    };

    // If we have enough samples in the fifo output buffer, pass them to the audio output.
    if (fifoOutputLastSample - fifoOutputFirstSample >= numberOfSamples) {
        SuperpoweredFloatToShortInt(fifoOutput + fifoOutputFirstSample * 2, output,
                                    numberOfSamples);
        fifoOutputFirstSample += numberOfSamples;
        return true;
    } else return false;

    // return !silence;
}

void HearingAidAudioProcessor::onPlayPause(bool play) {
    if (play)
        start();
    else
        stop();
}

void HearingAidAudioProcessor::onForeground() {
    audioSystem->onForeground();
}

void HearingAidAudioProcessor::onBackground() {
    audioSystem->onBackground();
}

void HearingAidAudioProcessor::start() {
    audioSystem->start();
}

void HearingAidAudioProcessor::stop() {
    audioSystem->stop();
}

extern "C" JNIEXPORT void
Java_com_github_vatbub_hearingaid_fragments_StreamingFragment_HearingAidAudioProcessor(
        JNIEnv *javaEnvironment, jobject __unused obj, jint samplerate,
        jint buffersize/*, jstring apkPath, jint fileAoffset, jint fileAlength, jint fileBoffset, jint fileBlength*/) {
    // const char *path = javaEnvironment->GetStringUTFChars(apkPath, JNI_FALSE);
    jniInstance = new HearingAidAudioProcessor((unsigned int) samplerate,
                                               (unsigned int) buffersize);
    // javaEnvironment->ReleaseStringUTFChars(apkPath, path);
}

extern "C" JNIEXPORT void Java_com_github_vatbub_hearingaid_fragments_StreamingFragment_onPlayPause(
        JNIEnv *__unused javaEnvironment, jobject __unused obj, jboolean play) {
    jniInstance->onPlayPause(play);
}
