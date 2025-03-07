package org.eflerrr.sfp.sparkjobs;

import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws StreamingQueryException, TimeoutException {
        Core.run();
    }
}
