package com.boydti.fawe.object;

public abstract class RunnableVal<T> implements Runnable {
    public T value;
    
    public RunnableVal() {}
    
    public RunnableVal(T value) {
        this.value = value;
    }

    @Override
    public void run() {
        run(this.value);
    }
    
    public abstract void run(T value);
}
