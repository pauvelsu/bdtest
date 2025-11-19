package es.ulpgc.searchengine;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class MicrobenchmarkApp {
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include("es.ulpgc.searchengine.FileSystemBenchmarks")
                .include("es.ulpgc.searchengine.TextBenchmarks")
                .warmupIterations(2)
                .measurementIterations(5)
                .forks(1)
                .build();

        new Runner(opt).run();
    }
}