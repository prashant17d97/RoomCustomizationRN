package com.whitelabel.android.imagerNative.gen;

import java.util.Collection;

public class IntVector {
    protected transient boolean swigCMemOwn;
    private transient long swigCPtr;

    /* JADX INFO: Access modifiers changed from: protected */
    public IntVector(long j, boolean z) {
        this.swigCMemOwn = z;
        this.swigCPtr = j;
    }

    protected static long getCPtr(IntVector intVector) {
        if (intVector == null) {
            return 0L;
        }
        return intVector.swigCPtr;
    }

    protected void finalize() {
        delete();
    }

    public synchronized void delete() {
        long j = this.swigCPtr;
        if (j != 0) {
            if (this.swigCMemOwn) {
                this.swigCMemOwn = false;
                ImagerNativeJNI.delete_IntVector(j);
            }
            this.swigCPtr = 0L;
        }
    }

    IntVector(Collection<Integer> collection) {
        reserve(collection.size());
        for (Integer num : collection) {
            push_back(num.intValue());
        }
    }

    public IntVector() {
        this(ImagerNativeJNI.new_IntVector__SWIG_0(), true);
    }

    public IntVector(long j) {
        this(ImagerNativeJNI.new_IntVector__SWIG_1(j), true);
    }

    public IntVector(IntVector intVector) {
        this(ImagerNativeJNI.new_IntVector__SWIG_2(getCPtr(intVector), intVector), true);
    }

    public long capacity() {
        return ImagerNativeJNI.IntVector_capacity(this.swigCPtr, this);
    }

    public void reserve(long j) {
        ImagerNativeJNI.IntVector_reserve(this.swigCPtr, this, j);
    }

    public boolean isEmpty() {
        return ImagerNativeJNI.IntVector_isEmpty(this.swigCPtr, this);
    }

    public void clear() {
        ImagerNativeJNI.IntVector_clear(this.swigCPtr, this);
    }

    public void push_back(int i) {
        ImagerNativeJNI.IntVector_push_back(this.swigCPtr, this, i);
    }

    public int get(int i) {
        return ImagerNativeJNI.IntVector_get(this.swigCPtr, this, i);
    }

    public int set(int i, int i2) {
        return ImagerNativeJNI.IntVector_set(this.swigCPtr, this, i, i2);
    }

    public int size() {
        return ImagerNativeJNI.IntVector_size(this.swigCPtr, this);
    }

    public void removeRange(int i, int i2) {
        ImagerNativeJNI.IntVector_removeRange(this.swigCPtr, this, i, i2);
    }
}
