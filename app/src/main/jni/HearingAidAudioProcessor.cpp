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

using namespace Superpowered;

static HearingAidAudioProcessor *jniInstance = NULL;

static FrequencyDomain *frequencyDomain;
static float *magnitudeLeft, *magnitudeRight, *phaseLeft, *phaseRight, *fifoOutput, *inputBufferFloat;
static int fifoOutputFirstSample, fifoOutputLastSample, stepSize, fifoCapacity;

#define FFT_LOG_SIZE 11 // 2^11 = 2048
#define FFT_SIZE 2048

static bool
audioProcessing(void *clientdata, short int *audioIO, int numberOfSamples, int samplerate) {
    return ((HearingAidAudioProcessor *) clientdata)->process(audioIO,
                                                              (unsigned int) numberOfSamples);
}

HearingAidAudioProcessor::HearingAidAudioProcessor(unsigned int samplerate,
                                                   unsigned int buffersize) {
    // Force rebuild
    frequencyDomain = new FrequencyDomain(
            FFT_LOG_SIZE); // This will do the main "magic".

    // In newer Superpowered versions, fftSize is not a member.
    // We use the constant FFT_SIZE derived from FFT_LOG_SIZE.
    stepSize = FFT_SIZE / 4; // The default overlap ratio is 4:1

    // Frequency domain data goes into these buffers:
    magnitudeLeft = (float *) malloc(FFT_SIZE * sizeof(float));
    magnitudeRight = (float *) malloc(FFT_SIZE * sizeof(float));
    phaseLeft = (float *) malloc(FFT_SIZE * sizeof(float));
    phaseRight = (float *) malloc(FFT_SIZE * sizeof(float));

    // Time domain result goes into a FIFO (first-in, first-out) buffer
    fifoOutputFirstSample = fifoOutputLastSample = 0;
    fifoCapacity = stepSize *
                   100; // Let's make the fifo's size 100 times more than the step size, so we save memory bandwidth.
    fifoOutput = (float *) malloc(fifoCapacity * sizeof(float) * 2 + 128);

    inputBufferFloat = (float *) malloc(buffersize * sizeof(float) * 2 + 128);

    audioSystem = new SuperpoweredAndroidAudioIO(samplerate, buffersize, true, true,
                                                 audioProcessing, this,
                                                 SL_ANDROID_RECORDING_PRESET_UNPROCESSED,
                                                 SL_ANDROID_STREAM_MEDIA);
    onPlayPause(false);
}

HearingAidAudioProcessor::~HearingAidAudioProcessor() {
    delete audioSystem;
}

bool HearingAidAudioProcessor::process(short int *output, unsigned int numberOfSamples) {
    // return if eq is disabled
    if (!eqEnabled)
        return true;

    ShortIntToFloat(output, inputBufferFloat,
                                numberOfSamples); // Converting the 16-bit integer samples to 32-bit floating point.
    frequencyDomain->addInput(inputBufferFloat,
                              numberOfSamples); // Input goes to the frequency domain.

    // In the frequency domain we are working with 1024 magnitudes and phases for every channel (left, right), if the fft size is 2048.
    while (frequencyDomain->timeDomainToFrequencyDomain(magnitudeLeft, magnitudeRight, phaseLeft,
                                                        phaseRight)) {
        // You can work with frequency domain data from this point.

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
        FloatToShortInt(fifoOutput + fifoOutputFirstSample * 2, output,
                                    numberOfSamples);
        fifoOutputFirstSample += numberOfSamples;
        return true;
    } else return false;
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
    CPU::setSustainedPerformanceMode(true);
    audioSystem->start();
}

void HearingAidAudioProcessor::stop() {
    CPU::setSustainedPerformanceMode(false);
    audioSystem->stop();
}

void HearingAidAudioProcessor::enableEQ(bool eqEnabled) {
    this->eqEnabled = eqEnabled;
}

extern "C" JNIEXPORT void
Java_com_github_vatbub_hearingaid_backend_HearingAidPlaybackService_HearingAidAudioProcessor(
        JNIEnv *javaEnvironment, jobject __unused obj, jint samplerate,
        jint buffersize/*, jstring apkPath, jint fileAoffset, jint fileAlength, jint fileBoffset, jint fileBlength*/) {
    // const char *path = javaEnvironment->GetStringUTFChars(apkPath, JNI_FALSE);
    jniInstance = new HearingAidAudioProcessor((unsigned int) samplerate,
                                               (unsigned int) buffersize);
    // javaEnvironment->ReleaseStringUTFChars(apkPath, path);
}

extern "C" JNIEXPORT void
Java_com_github_vatbub_hearingaid_backend_HearingAidPlaybackService_onPlayPause(
        JNIEnv *__unused javaEnvironment, jobject __unused obj, jboolean play) {
    jniInstance->onPlayPause(play);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_vatbub_hearingaid_backend_HearingAidPlaybackService_onBackground(JNIEnv *env,
                                                                                 jobject instance) {
    jniInstance->onBackground();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_github_vatbub_hearingaid_backend_HearingAidPlaybackService_onForeground(JNIEnv *env,
                                                                                 jobject instance) {
    jniInstance->onForeground();
}

extern "C" JNIEXPORT void
Java_com_github_vatbub_hearingaid_backend_HearingAidPlaybackService_eqEnabled(
        JNIEnv *__unused javaEnvironment, jobject __unused obj, jboolean eqEnabled) {
    jniInstance->enableEQ(eqEnabled);
}
