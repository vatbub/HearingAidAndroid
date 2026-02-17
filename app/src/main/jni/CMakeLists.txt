cmake_minimum_required(VERSION 3.4.1)

project(HearingAidAndroid)

set(
	PATH_TO_SUPERPOWERED
	CACHE STRING ""
)

message(${ANDROID_ABI})

add_library(
	HearingAidAudioProcessor
	SHARED
	HearingAidAudioProcessor.cpp
	${PATH_TO_SUPERPOWERED}/OpenSource/SuperpoweredAndroidAudioIO.cpp
)

include_directories(src/main/jni)
include_directories(${PATH_TO_SUPERPOWERED})

target_link_libraries(
    HearingAidAudioProcessor
    log
    android
    OpenSLES
    ${PATH_TO_SUPERPOWERED}/libSuperpoweredAndroid${ANDROID_ABI}.a
)