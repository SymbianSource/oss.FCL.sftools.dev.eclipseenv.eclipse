/*******************************************************************************
 * Copyright (c) 2002 - 2005 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *******************************************************************************/
#include <jni.h>
#include <stdio.h>
#include <PTYInputStream.h>
#include <PTYOutputStream.h>
#include <unistd.h>

/* Header for class _org_eclipse_cdt_utils_pty_PTYInputStream */
/* Header for class _org_eclipse_cdt_utils_pty_PTYOutputStream */

/*
 * Class:     org_eclipse_cdt_utils_pty_PTYInputStream
 * Method:    read0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_org_eclipse_cdt_utils_pty_PTYInputStream_read0(JNIEnv * env,
                                                          jobject jobj,
                                                          jint jfd,
                                                          jbyteArray buf,
                                                          jint buf_len)
{
    int fd;
    int status;
    jbyte *data;
    int data_len;

    data = (*env)->GetByteArrayElements(env, buf, 0);
    data_len = buf_len;
    fd = jfd;

    status = read( fd, data, data_len );
    (*env)->ReleaseByteArrayElements(env, buf, data, 0);

    if (status == 0) {
        /* EOF. */
        status = -1;
    } else if (status == -1) {
        /* Error, toss an exception */
        jclass exception = (*env)->FindClass(env, "java/io/IOException");
        if (exception == NULL) {
            /* Give up.  */
            return -1;
        }
        (*env)->ThrowNew(env, exception, "read error");
    }

    return status;
}


/*
 * Class:     org_eclipse_cdt_utils_pty_PTYInputStream
 * Method:    close0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_org_eclipse_cdt_utils_pty_PTYInputStream_close0(JNIEnv * env,
                                                           jobject jobj,
                                                           jint fd)
{
    return close(fd);
}

/*
 * Class:     org_eclipse_cdt_utils_pty_PTYOutputStream
 * Method:    write0
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL
Java_org_eclipse_cdt_utils_pty_PTYOutputStream_write0(JNIEnv * env,
                                                            jobject jobj,
                                                            jint jfd,
                                                            jbyteArray buf,
                                                            jint buf_len)
{
    int status;
    int fd;
    jbyte *data;
    int data_len;

    data = (*env)->GetByteArrayElements(env, buf, 0);
    data_len = buf_len;
    fd = jfd;

    status = write(fd, data, data_len);
    (*env)->ReleaseByteArrayElements(env, buf, data, 0);

    return status;
}


/*
 * Class:     org_eclipse_cdt_utils_pty_PTYOutputStream
 * Method:    close0
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL
Java_org_eclipse_cdt_utils_pty_PTYOutputStream_close0(JNIEnv * env,
                                                            jobject jobj,
                                                            jint fd)
{
    return close(fd);
}
