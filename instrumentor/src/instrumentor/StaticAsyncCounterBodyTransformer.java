package instrumentor;

import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.annotation.logic.Loop;
import soot.toolkits.graph.LoopNestTree;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

public class StaticAsyncCounterBodyTransformer extends BodyTransformer {

    private SootClass asyncTaskCounterClass, handlerClass, messageClass;
    private SootMethod incrementAsyncTaskMethod, incrementMsgTaskMethod, getCurrentEventIdMethod, setCurrentEventIdMethod, checkAndSetEventIdMethod;

    private String packageName;
    private static File staticAnalysisResultFile = null;
    private static int async = 0;
    private static boolean hasLoop = false;



    public static void main(String[] args) {
        // args[0]: directory from which to process classes
        // args[1]: path for finding the android.jar file
        // args[2]: package name of the app to be instrumented (full apk file name for this code)

        if(args.length < 3) {
            System.out.println("Please enter the args: <process-dir> <android-jars> <app-package-name>");
            return;
        }

        //packageName = args[2];

        PackManager.v().getPack("jtp").add(
                new Transform("jtp.myInstrumenter", new StaticAsyncCounterBodyTransformer(args[2])));

        soot.Main.main(new String[]{
                "-debug",
                "-prepend-classpath",
                "-process-dir", args[0],
                "-android-jars", args[1],
                "-src-prec", "apk",
                "-output-format", "dex",
                "-allow-phantom-refs"
        });
    }

    public StaticAsyncCounterBodyTransformer(String packName) {
        packageName = packName;
        if(packageName != null && packageName.length() > 5)
            packageName = packageName.substring(5, packName.length()-5);
    }

    private void initTransformer() {

        Scene.v().addBasicClass("counter.AsyncTaskCounter", SootClass.BODIES);
        asyncTaskCounterClass = Scene.v().getSootClass("counter.AsyncTaskCounter");
        asyncTaskCounterClass.setApplicationClass();

        getCurrentEventIdMethod = asyncTaskCounterClass.getMethodByName("getCurrentEventId");
        checkAndSetEventIdMethod = asyncTaskCounterClass.getMethodByName("checkAndSetEventId");
        setCurrentEventIdMethod = asyncTaskCounterClass.getMethodByName("setCurrentEventId");
        incrementAsyncTaskMethod = asyncTaskCounterClass.getMethodByName("incrementAsyncTask");
        incrementMsgTaskMethod = asyncTaskCounterClass.getMethodByName("incrementMsgTask");

        handlerClass = Scene.v().getSootClass("android.os.Handler");
        messageClass = Scene.v().getSootClass("android.os.Message");

        staticAnalysisResultFile = new File("out", packageName.concat(".txt"));
    }

    @Override
    protected void internalTransform(final Body b, String phaseName,
                                     @SuppressWarnings("rawtypes") Map options) {

        initTransformer();

        String className = b.getMethod().getDeclaringClass().toString();
        String methodName = b.getMethod().getName();

        if(!className.startsWith("android.support"))
        {
            // check whether multiple asynctasks are spawned inside a task
            staticCheckAsyncTasks(b, methodName, className);
        }

    }

    private void staticCheckAsyncTasks(Body b, final String methodName, final String className) {

        async = 0;
        hasLoop = false;

        LoopNestTree loopNestTree = new LoopNestTree(b);
        for (Loop loop : loopNestTree) {
            System.out.println("Found a loop with head: " + loop.getHead());
            hasLoop = true;
        }

        final PatchingChain<Unit> units = b.getUnits();
        Iterator<Unit> iter = units.snapshotIterator();

        while (iter.hasNext()) {
            iter.next().apply(new AbstractStmtSwitch() {

                public void caseInvokeStmt(InvokeStmt stmt) {
                    String invokedMethodName = stmt.getInvokeExpr().getMethod().getName();
                    SootClass invokedMethodClass = stmt.getInvokeExpr().getMethod().getDeclaringClass();
                    String invokedMethodClassName = invokedMethodClass.getName();

                    if ((((invokedMethodName.equals("execute") || invokedMethodName.equals("executeOnExecutor")) && invokedMethodClassName.equals("android.os.AsyncTask")))
                       || (invokedMethodName.equals("execute") && invokedMethodClassName.equals("java.util.concurrent.ThreadPoolExecutor"))
                       || (invokedMethodName.contains("sendMessage") && SootUtils.hasParentClass(invokedMethodClass, handlerClass))
                       || (invokedMethodName.contains("post") && SootUtils.hasParentClass(invokedMethodClass, handlerClass))
                       || (invokedMethodName.contains("sendToTarget") && SootUtils.hasParentClass(invokedMethodClass, messageClass))
                       || (invokedMethodName.equals("start") && invokedMethodClassName.equals("java.lang.Thread"))) {

                        async ++;
                        if(async >= 2 || (async >= 1 && hasLoop)) {

                            String s = "".concat(invokedMethodName).concat(" in method: ").concat(methodName).concat(" in class: ").concat(className).concat("\n");
                            try {
                                PrintWriter writer = new PrintWriter(staticAnalysisResultFile);
                                writer.append(s);
                                writer.flush();
                                writer.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }

                    }
                }
            });
        }
    }



}
