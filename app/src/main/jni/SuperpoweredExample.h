#ifndef Header_SuperpoweredExample
#define Header_SuperpoweredExample

#include <math.h>
#include <pthread.h>

#include <SuperpoweredAdvancedAudioPlayer.h>
#include <SuperpoweredFilter.h>
#include <SuperpoweredRoll.h>
#include <SuperpoweredFlanger.h>
#include <OpenSource/SuperpoweredAndroidAudioIO.h>

using namespace Superpowered;

#define HEADROOM_DECIBEL 3.0f
static const float headroom = powf(10.0f, -HEADROOM_DECIBEL * 0.025f);

class SuperpoweredExample {
public:

	SuperpoweredExample(unsigned int samplerate, unsigned int buffersize, const char *path, int fileAoffset, int fileAlength, int fileBoffset, int fileBlength);
	~SuperpoweredExample();

	bool process(short int *output, unsigned int numberOfSamples);
	void onPlayPause(bool play);
	void onCrossfader(int value);
	void onFxSelect(int value);
	void onFxOff();
	void onFxValue(int value);
    void onEQBand(unsigned int index, int gain);

private:
    SuperpoweredAndroidAudioIO *audioSystem;
    AdvancedAudioPlayer *playerA, *playerB;
    Roll *roll;
    Filter *filter;
    Flanger *flanger;
    float *stereoBuffer;
    unsigned char activeFx;
    float crossValue, volA, volB;
};

#endif
