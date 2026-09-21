package de.mrjulsen.crn.web;

final class RequestMetrics {
    long queueWaitNanos = -1;
    long buildNanos;
    long serializeNanos;
    long payloadBytes;
    long sentBytes;
}
