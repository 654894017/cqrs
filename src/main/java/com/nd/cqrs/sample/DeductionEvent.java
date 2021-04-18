package com.nd.cqrs.sample;

import com.nd.cqrs.domain.Event;

public class DeductionEvent extends Event {



    private int count;

    public DeductionEvent() {
        super();
    }

    public DeductionEvent( int count) {
        this.count = count;
    }


    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

}