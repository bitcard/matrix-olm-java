/*
 * Copyright 2018 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _OMLPK_H
#define _OMLPK_H

#include "olm_jni.h"
#include "olm/pk.h"

#define OLM_PK_ENCRYPTION_FUNC_DEF(func_name) FUNC_DEF(OlmPkEncryption,func_name)
#define OLM_PK_DECRYPTION_FUNC_DEF(func_name) FUNC_DEF(OlmPkDecryption,func_name)

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong OLM_PK_ENCRYPTION_FUNC_DEF(createNewPkEncryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_ENCRYPTION_FUNC_DEF(releasePkEncryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_ENCRYPTION_FUNC_DEF(setRecipientKeyJni)(JNIEnv *env, jobject thiz, jbyteArray aKeyBuffer);

JNIEXPORT jbyteArray OLM_PK_ENCRYPTION_FUNC_DEF(encryptJni)(JNIEnv *env, jobject thiz, jbyteArray aPlaintextBuffer, jobject aEncryptedMsg);

JNIEXPORT jlong OLM_PK_DECRYPTION_FUNC_DEF(createNewPkDecryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT void OLM_PK_DECRYPTION_FUNC_DEF(releasePkDecryptionJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(generateKeyJni)(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray OLM_PK_DECRYPTION_FUNC_DEF(decryptJni)(JNIEnv *env, jobject thiz, jobject aEncryptedMsg);

#ifdef __cplusplus
}
#endif

#endif
