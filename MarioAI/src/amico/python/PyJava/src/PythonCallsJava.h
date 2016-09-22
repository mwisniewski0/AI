/**
 @file PythonCallsJava.h
 
 @brief In this file declared functions, that are exported from the library.

 @author Sergey.Karakovskiy at idsia.ch, Nikolay.Sohryakov at gmail.com
 
 You can add your own functions to be exportd in the section @e Custom @e Code at the end of the file.
 Functions declared here are implemented in @e PythonCallsJava.cc file.
 */
#include "arrayutils.h"
#ifndef _PYTHONCALLSJAVA_H
#define	_PYTHONCALLSJAVA_H

#ifdef __cplusplus
extern "C" {
#endif

void amicoInitialize(int nOptions = 0, ...);
void destroyEnvironment();

/*
 * Mario AI Specific methods:
 */
void initMarioAIBenchmark();
void createMarioEnvironment(const char* javaClassName);
void reset(char* setUpOptions);
bool isLevelFinished();
void tick();
PyObject* getEvaluationInfo() ;
PyObject* buildPythonTuple(JNIEnv* env,
                           jintArray serializedLevelScene,
                           jintArray serializedEnemies,
                           jfloatArray marioPos,
                           jfloatArray enemiesPos,
                           jintArray marioState);
PyObject* getEntireObservation(int zLevelScene, int zLevelEnemies);
void performAction(int* action);
PyObject* getReceptiveFieldInfo();
/*
 *
 */

#ifdef __cplusplus
}
#endif


#endif	/* _PYTHONCALLSJAVA_H */

