package counter;

import java.lang.StringBuilder;

import android.util.Log;

public class AsyncTaskCounter {

    private static int currentEventId = 0;
    private static int maxEventId = 0;

    // counters of tasks for 500 events
    private static final int MAX_EVENT_COUNT = 500;
    private static int asyncTaskCounters[] = new int[MAX_EVENT_COUNT];
    private static int msgTaskCounters[] = new int[MAX_EVENT_COUNT];

    // for formatted logging purposes
    private static int inner = 0;

    public static void setCurrentEventId(int id, String methodName, String className) {
        StringBuilder s = new StringBuilder("");

        if(id == 0) {
            Log.v("ASYNC", s.append("Completed  event: ").append(methodName).append(" ").append(className).append(" ").append(currentEventId).toString());
            logCounter(methodName, className);
            inner = 0;
        } else {
            for(int i=0; i<inner; i++){
                s.append("  ");
            }
            Log.v("ASYNC", s.append("Completed  event: ").append(methodName).append(" ").append(className).append(" ").append(currentEventId).toString());
            inner--;
        }

        currentEventId = id;
    }

    public static void checkAndSetEventId(String methodName, String className) {
        StringBuilder s = new StringBuilder("");
        if(currentEventId > 0) {
            inner ++;
            for(int i=0; i<inner; i++){
                s.append("  ");
            }
            Log.v("ASYNC", s.append("Already in event: ").append(methodName).append(" ").append(className).append(" ").append(currentEventId).toString());
        } else {
            currentEventId = maxEventId + 1;
            maxEventId = currentEventId;
            Log.v("ASYNC", s.append("Started event: ").append(methodName).append(" ").append(className).append(" ").append(currentEventId).toString());
        }
    }

    public static int getCurrentEventId() {
        return currentEventId;
    }

    public static void incrementAsyncTask() {
        if(currentEventId < MAX_EVENT_COUNT)
            asyncTaskCounters[currentEventId] ++;
        else
            Log.i("ASYNC", "Exceeded 100 events.. Not counting anymore");
        Log.i("ASYNC", "Incremented the asynctask of the event: " + currentEventId + " to: " + asyncTaskCounters[currentEventId]);
    }

    public static void incrementMsgTask() {
        if(currentEventId < MAX_EVENT_COUNT)
            msgTaskCounters[currentEventId] ++;
        else
            Log.i("ASYNC", "Exceeded 100 events.. Not counting anymore");
        Log.i("ASYNC", "Incremented the msgs of the event: " + currentEventId + " to: " + msgTaskCounters[currentEventId]);
    }

    public static void logCounter(String methodName, String className) {
        if ((asyncTaskCounters[currentEventId] + msgTaskCounters[currentEventId]) > 0) {
            Log.i("ASYNC", "" + asyncTaskCounters[currentEventId] + "# of AsyncTasks executed in event: " + currentEventId + " " + methodName + " in class: " + className);
            Log.i("ASYNC", "" + msgTaskCounters[currentEventId] + "# of messages are sent in event: " + currentEventId + " " + methodName + " in class: " + className);
        }
    }

}