package com.whitelabel.android.imagerNative.gen;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class PaintMask {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    protected PaintMask(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    protected static long getCPtr(PaintMask paintMask) {
        if (paintMask == null) {
            return 0L;
        }
        return paintMask.swigCPtr;
    }

    protected void finalize() {
        delete();
    }

    public synchronized void delete() {
        long j = this.swigCPtr;
        if (j != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ImagerNativeJNI.delete_PaintMask(j);
            }
            this.swigCPtr = 0L;
        }
    }

    public PaintMask(Mat mat) {
        this(ImagerNativeJNI.new_PaintMask__SWIG_0(mat.getNativeObjAddr()), true);
    }

    public PaintMask(PaintMask paintMask) {
        this(ImagerNativeJNI.new_PaintMask__SWIG_1(getCPtr(paintMask), paintMask), true);
    }

    public Mat image() {
        return new Mat(ImagerNativeJNI.PaintMask_image(this.swigCPtr, this));
    }

    public void setAreas(PaintAreaVector paintAreaVector) {
        ImagerNativeJNI.PaintMask_setAreas(this.swigCPtr, this, PaintAreaVector.getCPtr(paintAreaVector), paintAreaVector);
    }

    public PaintAreaVector getAreas() {
        return new PaintAreaVector(ImagerNativeJNI.PaintMask_getAreas(this.swigCPtr, this), true);
    }

    public void setColor(int i) {
        ImagerNativeJNI.PaintMask_setColor(this.swigCPtr, this, i);
    }

    public int getColor() {
        return ImagerNativeJNI.PaintMask_getColor(this.swigCPtr, this);
    }

    public void setCoverage(float f) {
        ImagerNativeJNI.PaintMask_setCoverage(this.swigCPtr, this, f);
    }

    public float getCoverage() {
        return ImagerNativeJNI.PaintMask_getCoverage(this.swigCPtr, this);
    }

    public void setExclusionMask(Mat mat) {
        ImagerNativeJNI.PaintMask_setExclusionMask(this.swigCPtr, this, mat.getNativeObjAddr());
    }

    public void setExclusionMaskDisabled(boolean z) {
        ImagerNativeJNI.PaintMask_setExclusionMaskDisabled(this.swigCPtr, this, z);
    }

    public boolean didRepaint() {
        return ImagerNativeJNI.PaintMask_didRepaint(this.swigCPtr, this);
    }

    public boolean willDoRepaintForArea(PaintArea paintArea) {
        return ImagerNativeJNI.PaintMask_willDoRepaintForArea(this.swigCPtr, this, PaintArea.getCPtr(paintArea), paintArea);
    }

    public void setFreehandImage(Mat mat) {
        ImagerNativeJNI.PaintMask_setFreehandImage(this.swigCPtr, this, mat.getNativeObjAddr());
    }

    public void setAllowRepaint(boolean z) {
        ImagerNativeJNI.PaintMask_setAllowRepaint(this.swigCPtr, this, z);
    }

    public Mat floodImage() {
        return new Mat(ImagerNativeJNI.PaintMask_floodImage(this.swigCPtr, this));
    }

    public static Mat exclusionMaskFromImage(Mat mat) {
        return new Mat(ImagerNativeJNI.PaintMask_exclusionMaskFromImage(mat.getNativeObjAddr()));
    }

    public static void invertAlpha(Mat mat) {
        ImagerNativeJNI.PaintMask_invertAlpha(mat.getNativeObjAddr());
    }

    public static Rect findContourRect(Mat mat) {
        return ImagerNativeJNI.PaintMask_findContourRect(mat.getNativeObjAddr());
    }

    public static IntVector findHues(Mat mat) {
        return new IntVector(ImagerNativeJNI.PaintMask_findHues(mat.getNativeObjAddr()), true);
    }
}
