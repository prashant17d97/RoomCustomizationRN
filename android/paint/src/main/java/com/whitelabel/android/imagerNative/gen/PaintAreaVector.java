package com.whitelabel.android.imagerNative.gen;

import java.util.Collection;

public class PaintAreaVector {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    
    public PaintAreaVector(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    
    public static long getCPtr(PaintAreaVector paintAreaVector) {
        if (paintAreaVector == null) {
            return 0L;
        }
        return paintAreaVector.swigCPtr;
    }

    protected void finalize() {
        delete();
    }

    public synchronized void delete() {
        long j = this.swigCPtr;
        if (j != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ImagerNativeJNI.delete_PaintAreaVector(j);
            }
            this.swigCPtr = 0L;
        }
    }

    PaintAreaVector(Collection<PaintArea> collection) {
        reserve(collection.size());
        for (PaintArea paintArea : collection) {
            push_back(paintArea);
        }
    }

    public PaintAreaVector() {
        this(ImagerNativeJNI.new_PaintAreaVector__SWIG_0(), true);
    }

    public PaintAreaVector(long j) {
        this(ImagerNativeJNI.new_PaintAreaVector__SWIG_1(j), true);
    }

    public PaintAreaVector(PaintAreaVector paintAreaVector) {
        this(ImagerNativeJNI.new_PaintAreaVector__SWIG_2(getCPtr(paintAreaVector), paintAreaVector), true);
    }

    public long capacity() {
        return ImagerNativeJNI.PaintAreaVector_capacity(this.swigCPtr, this);
    }

    public void reserve(long j) {
        ImagerNativeJNI.PaintAreaVector_reserve(this.swigCPtr, this, j);
    }

    public boolean isEmpty() {
        return ImagerNativeJNI.PaintAreaVector_isEmpty(this.swigCPtr, this);
    }

    public void clear() {
        ImagerNativeJNI.PaintAreaVector_clear(this.swigCPtr, this);
    }

    public void push_back(PaintArea paintArea) {
        ImagerNativeJNI.PaintAreaVector_push_back(this.swigCPtr, this, PaintArea.getCPtr(paintArea), paintArea);
    }

    public PaintArea get(int i) {
        return new PaintArea(ImagerNativeJNI.PaintAreaVector_get(this.swigCPtr, this, i), false);
    }

    public PaintArea set(int i, PaintArea paintArea) {
        return new PaintArea(ImagerNativeJNI.PaintAreaVector_set(this.swigCPtr, this, i, PaintArea.getCPtr(paintArea), paintArea), true);
    }

    public int size() {
        return ImagerNativeJNI.PaintAreaVector_size(this.swigCPtr, this);
    }

    public void removeRange(int i, int i2) {
        ImagerNativeJNI.PaintAreaVector_removeRange(this.swigCPtr, this, i, i2);
    }
}
