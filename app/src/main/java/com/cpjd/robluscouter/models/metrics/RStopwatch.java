package com.cpjd.robluscouter.models.metrics;

import org.codehaus.jackson.annotate.JsonTypeName;

import java.util.ArrayList;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a time value, in seconds.
 * @see com.cpjd.robluscouter.models.metrics.RMetric for more information
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonTypeName("RStopwatch")
public class RStopwatch extends RMetric {

    /**
     * Changing this versionUID will render this class incompatible with older versions.
     */
    public static final long serialVersionUID = 1L;
    /**
     * The time, in seconds, currently on this stopwatch
     */
    private double time;

    /**
     * The time, in seconds, currently on this stopwatch.
     * This is now an array, since laps have been implemented
     */
    private ArrayList<Double> times;

    /**
     * The empty constructor is required for de-serialization
     */
    @SuppressWarnings("unused")
    public RStopwatch() {}

    /**
     * Instantiates a stopwatch object
     * @param ID the unique identifier for this object
     * @param title object title
     * @param time the time, in seconds
     */
    private RStopwatch(int ID, String title, double time) {
        super(ID, title);
        this.time = time;
    }

    @Override
    public String getFormDescriptor() {
        return "Type: Stopwatch\nDefault value: "+time;
    }

    @Override
    public RMetric clone() {
        RStopwatch stopwatch = new RStopwatch(ID, title, time);
        stopwatch.setTimes(times);
        return stopwatch;
    }
    @Override
    public String toString() {
        return String.valueOf(time);
    }
}
