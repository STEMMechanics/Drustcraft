package com.stemcraft;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SMTimer {
    private int sleepTicks = 0;

    public void sleep() {
        sleepTicks = 100;
    }

    public void sleep(int ticks) {
        sleepTicks = ticks;
    }

    public void processTimer() {
        if(sleepTicks <= 0) {
            runTimer();
        } else {
            sleepTicks--;
        }
    }

    public void runTimer() {

    }
}
