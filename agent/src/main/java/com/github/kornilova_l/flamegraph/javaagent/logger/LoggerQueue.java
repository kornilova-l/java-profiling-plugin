package com.github.kornilova_l.flamegraph.javaagent.logger;

import com.github.kornilova_l.flamegraph.javaagent.logger.event_data_storage.MethodEventData;
import com.github.kornilova_l.flamegraph.javaagent.logger.event_data_storage.RetValEventData;
import com.github.kornilova_l.flamegraph.javaagent.logger.event_data_storage.StartData;
import com.github.kornilova_l.flamegraph.javaagent.logger.event_data_storage.ThrowableEventData;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggerQueue {
    private static LoggerQueue loggerQueue;
    private final ConcurrentLinkedQueue<MethodEventData> queue = new ConcurrentLinkedQueue<>();
    private AtomicInteger countEventsAdded = new AtomicInteger(0);

    /**
     * Method is called by javaagent.
     * It is needed for loading LoggerQueue by system classLoader
     */
    public static void initLoggerQueue() {
        loggerQueue = new LoggerQueue();
    }

    static LoggerQueue getInstance() {
        return loggerQueue;
    }

    /**
     * This method is called concurrently
     */
    public static StartData createStartData(long startTime, Object[] parameters) {
        return new StartData(startTime, parameters);
    }

    /**
     * This method is called concurrently
     */
    public static void addToQueue(Object retVal,
                                  long startTime,
                                  long duration,
                                  Object[] parameters,
                                  Thread thread,
                                  String className,
                                  String methodName,
                                  String desc,
                                  boolean isStatic,
                                  String savedParameters) {
        loggerQueue.addToQueue(new RetValEventData(thread, className, startTime, duration,
                methodName, desc, isStatic, parameters, retVal, savedParameters));
    }

    /**
     * This method is called concurrently
     */
    public static void addToQueue(Throwable throwable,
                                  boolean saveMessage,
                                  long startTime,
                                  long duration,
                                  Object[] parameters,
                                  Thread thread,
                                  String className,
                                  String methodName,
                                  boolean isStatic,
                                  String desc,
                                  String savedParameters) {
        loggerQueue.addToQueue(new ThrowableEventData(thread, className, startTime, duration,
                methodName, desc, isStatic, parameters, throwable, saveMessage, savedParameters));
    }

    ConcurrentLinkedQueue<MethodEventData> getQueue() {
        return queue;
    }

    /**
     * This method is called concurrently
     */
    public void addToQueue(MethodEventData methodEventData) {
        countEventsAdded.incrementAndGet();
        queue.add(methodEventData);
    }

    int getEventsAdded() {
        return countEventsAdded.get();
    }
}
