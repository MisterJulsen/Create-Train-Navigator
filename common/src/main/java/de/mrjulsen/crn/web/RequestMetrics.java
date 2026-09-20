package de.mrjulsen.crn.web;

final class RequestMetrics {

    private static final ThreadLocal<Long> QUEUE_WAIT_NANOS = new ThreadLocal<>();

    /** How long the request waited in the pool queue, or {@code -1} if it was not measured. */
    long queueWaitNanos = -1;
    /** How long assembling the response from the backend took (handler + result shaping). */
    long buildNanos;
    /** How long turning the response into JSON bytes took. */
    long serializeNanos;
    /** How long gzip compression took, or {@code 0} if the response was not compressed. */
    long gzipNanos;
    /** The uncompressed response size in bytes. */
    long payloadBytes;
    /** The number of bytes actually written, after any compression. */
    long sentBytes;
    /** Whether the response was sent gzip-compressed. */
    boolean gzipped;

    /** Records, for the current worker thread, how long a task waited after it was submitted. */
    static void recordQueueWait(long submitNanos) {
        QUEUE_WAIT_NANOS.set(System.nanoTime() - submitNanos);
    }

    /** Reads and clears the queue-wait recorded for the current worker thread. */
    static long takeQueueWaitNanos() {
        Long value = QUEUE_WAIT_NANOS.get();
        if (value == null) {
            return -1;
        }
        QUEUE_WAIT_NANOS.remove();
        return value;
    }
}
