//
// Created by frede on 10.01.2018.
//

#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <SLES/OpenSLES.h>
#include <jni.h>
#include "HearingAidAudioProcessor.h"

static HearingAidAudioProcessor *jniInstance = NULL;

static bool
audioProcessing(void *clientdata, short int *audioIO, int numberOfSamples, int samplerate) {
    return ((HearingAidAudioProcessor *) clientdata)->process(audioIO,
                                                              (unsigned int) numberOfSamples);
}

HearingAidAudioProcessor::HearingAidAudioProcessor(unsigned int samplerate,
                                                   unsigned int buffersize) {
    audioSystem = new SuperpoweredAndroidAudioIO(samplerate, buffersize, true, true,
                                                 audioProcessing, this,
                                                 SL_ANDROID_RECORDING_PRESET_VOICE_COMMUNICATION,
                                                 SL_ANDROID_STREAM_MEDIA, buffersize * 2);
    silence = true;
}

HearingAidAudioProcessor::~HearingAidAudioProcessor() {
    delete audioSystem;
}

bool HearingAidAudioProcessor::process(short int *output, unsigned int numberOfSamples) {
    return !silence;
}

void HearingAidAudioProcessor::onPlayPause(bool play) {
    silence = !play;
}

extern "C" JNIEXPORT void Java_com_github_vatbub_hearingaid_fragments_StreamingFragment_HearingAidAudioProcessor(JNIEnv *javaEnvironment, jobject __unused obj, jint samplerate, jint buffersize/*, jstring apkPath, jint fileAoffset, jint fileAlength, jint fileBoffset, jint fileBlength*/) {
    // const char *path = javaEnvironment->GetStringUTFChars(apkPath, JNI_FALSE);
    jniInstance = new HearingAidAudioProcessor((unsigned int)samplerate, (unsigned int)buffersize);
    // javaEnvironment->ReleaseStringUTFChars(apkPath, path);
}

extern "C" JNIEXPORT void Java_com_github_vatbub_hearingaid_fragments_StreamingFragment_onPlayPause(JNIEnv * __unused javaEnvironment, jobject __unused obj, jboolean play) {
    jniInstance->onPlayPause(play);
}
