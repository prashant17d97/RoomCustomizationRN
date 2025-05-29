package com.whitelabel.android.imagerNative.gen;

import org.opencv.core.Rect;

public class ImagerNativeJNI {
    public static final native long IntVector_capacity(long j, IntVector intVector);

    public static final native void IntVector_clear(long j, IntVector intVector);

    public static final native int IntVector_get(long j, IntVector intVector, int i);

    public static final native boolean IntVector_isEmpty(long j, IntVector intVector);

    public static final native void IntVector_push_back(long j, IntVector intVector, int i);

    public static final native void IntVector_removeRange(long j, IntVector intVector, int i, int i2);

    public static final native void IntVector_reserve(long j, IntVector intVector, long j2);

    public static final native int IntVector_set(long j, IntVector intVector, int i, int i2);

    public static final native int IntVector_size(long j, IntVector intVector);

    public static final native long PaintAreaVector_capacity(long j, PaintAreaVector paintAreaVector);

    public static final native void PaintAreaVector_clear(long j, PaintAreaVector paintAreaVector);

    public static final native long PaintAreaVector_get(long j, PaintAreaVector paintAreaVector, int i);

    public static final native boolean PaintAreaVector_isEmpty(long j, PaintAreaVector paintAreaVector);

    public static final native void PaintAreaVector_push_back(long j, PaintAreaVector paintAreaVector, long j2, PaintArea paintArea);

    public static final native void PaintAreaVector_removeRange(long j, PaintAreaVector paintAreaVector, int i, int i2);

    public static final native void PaintAreaVector_reserve(long j, PaintAreaVector paintAreaVector, long j2);

    public static final native long PaintAreaVector_set(long j, PaintAreaVector paintAreaVector, int i, long j2, PaintArea paintArea);

    public static final native int PaintAreaVector_size(long j, PaintAreaVector paintAreaVector);

    public static final native float PaintArea_threshold_get(long j, PaintArea paintArea);

    public static final native void PaintArea_threshold_set(long j, PaintArea paintArea, float f);

    public static final native double PaintArea_x_get(long j, PaintArea paintArea);

    public static final native void PaintArea_x_set(long j, PaintArea paintArea, double d);

    public static final native double PaintArea_y_get(long j, PaintArea paintArea);

    public static final native void PaintArea_y_set(long j, PaintArea paintArea, double d);

    public static final native boolean PaintMask_didRepaint(long j, PaintMask paintMask);

    public static final native long PaintMask_exclusionMaskFromImage(long j);

    public static final native Rect PaintMask_findContourRect(long j);

    public static final native long PaintMask_findHues(long j);

    public static final native long PaintMask_floodImage(long j, PaintMask paintMask);

    public static final native long PaintMask_getAreas(long j, PaintMask paintMask);

    public static final native int PaintMask_getColor(long j, PaintMask paintMask);

    public static final native float PaintMask_getCoverage(long j, PaintMask paintMask);

    public static final native long PaintMask_image(long j, PaintMask paintMask);

    public static final native void PaintMask_invertAlpha(long j);

    public static final native void PaintMask_setAllowRepaint(long j, PaintMask paintMask, boolean z);

    public static final native void PaintMask_setAreas(long j, PaintMask paintMask, long j2, PaintAreaVector paintAreaVector);

    public static final native void PaintMask_setColor(long j, PaintMask paintMask, int i);

    public static final native void PaintMask_setCoverage(long j, PaintMask paintMask, float f);

    public static final native void PaintMask_setExclusionMask(long j, PaintMask paintMask, long j2);

    public static final native void PaintMask_setExclusionMaskDisabled(long j, PaintMask paintMask, boolean z);

    public static final native void PaintMask_setFreehandImage(long j, PaintMask paintMask, long j2);

    public static final native boolean PaintMask_willDoRepaintForArea(long j, PaintMask paintMask, long j2, PaintArea paintArea);

    public static final native void delete_IntVector(long j);

    public static final native void delete_PaintArea(long j);

    public static final native void delete_PaintAreaVector(long j);

    public static final native void delete_PaintMask(long j);

    public static final native long new_IntVector__SWIG_0();

    public static final native long new_IntVector__SWIG_1(long j);

    public static final native long new_IntVector__SWIG_2(long j, IntVector intVector);

    public static final native long new_PaintAreaVector__SWIG_0();

    public static final native long new_PaintAreaVector__SWIG_1(long j);

    public static final native long new_PaintAreaVector__SWIG_2(long j, PaintAreaVector paintAreaVector);

    public static final native long new_PaintArea__SWIG_0();

    public static final native long new_PaintArea__SWIG_1(double d, double d2, float f);

    public static final native long new_PaintArea__SWIG_2(double d, double d2);

    public static final native long new_PaintMask__SWIG_0(long j);

    public static final native long new_PaintMask__SWIG_1(long j, PaintMask paintMask);
}
