package org.eflerrr.sfp.sparkjobs.verifier;

import org.apache.spark.sql.streaming.StreamingQueryException;

import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) throws StreamingQueryException, TimeoutException {
        Verifier.run();
    }
}
