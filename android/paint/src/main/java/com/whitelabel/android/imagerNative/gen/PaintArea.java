package com.whitelabel.android.imagerNative.gen;

public class PaintArea {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    /* JADX INFO: Access modifiers changed from: protected */
    public PaintArea(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    public static long getCPtr(PaintArea paintArea) {
        if (paintArea == null) {
            return 0L;
        }
        return paintArea.swigCPtr;
    }

    protected void finalize() {
        delete();
    }

    public synchronized void delete() {
        long j = this.swigCPtr;
        if (j != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ImagerNativeJNI.delete_PaintArea(j);
            }
            this.swigCPtr = 0L;
        }
    }

    public PaintArea() {
        this(ImagerNativeJNI.new_PaintArea__SWIG_0(), true);
    }

    public PaintArea(double d, double d2, float f) {
        this(ImagerNativeJNI.new_PaintArea__SWIG_1(d, d2, f), true);
    }

    public PaintArea(double d, double d2) {
        this(ImagerNativeJNI.new_PaintArea__SWIG_2(d, d2), true);
    }

    public void setX(double d) {
        ImagerNativeJNI.PaintArea_x_set(this.swigCPtr, this, d);
    }

    public double getX() {
        return ImagerNativeJNI.PaintArea_x_get(this.swigCPtr, this);
    }

    public void setY(double d) {
        ImagerNativeJNI.PaintArea_y_set(this.swigCPtr, this, d);
    }

    public double getY() {
        return ImagerNativeJNI.PaintArea_y_get(this.swigCPtr, this);
    }

    public void setThreshold(float f) {
        ImagerNativeJNI.PaintArea_threshold_set(this.swigCPtr, this, f);
    }

    public float getThreshold() {
        return ImagerNativeJNI.PaintArea_threshold_get(this.swigCPtr, this);
    }
}
