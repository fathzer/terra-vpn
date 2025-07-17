package com.fathzer.odvpn.utils;

import java.io.IOException;

public class IOLambdas {
    private IOLambdas() {
    }

    @FunctionalInterface
    public static interface IORunnable {
        void run() throws IOException;
    }

    @FunctionalInterface
    public static interface IOFunction<T, R> {
        R apply(T t) throws IOException;
    }

    @FunctionalInterface
    public static interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    @FunctionalInterface
    public static interface IOBiConsumer<T, U> {
        void accept(T t, U u) throws IOException;
    }

	@FunctionalInterface
	public static interface IOPredicate<T> {
		boolean test(T t) throws IOException;
	}
}
