package instrumentor;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class AsyncCounterBodyTransformer extends BodyTransformer {

    private SootClass asyncTaskCounterClass, handlerClass, messageClass;
    private SootMethod incrementAsyncTaskMethod, incrementMsgTaskMethod, getCurrentEventIdMethod, setCurrentEventIdMethod, checkAndSetEventIdMethod;

    private String packageName;


    public static void main(String[] args) {
        // args[0]: directory from which to process classes
        // args[1]: path for finding the android.jar file
        // args[2]: package name of the app to be instrumented

        if(args.length < 3) {
            System.out.println("Please enter the args: <process-dir> <android-jars> <app-package-name>");
            return;
        }

        //packageName = args[2];

        PackManager.v().getPack("jtp").add(
                new Transform("jtp.myInstrumenter", new AsyncCounterBodyTransformer(args[2])));

        soot.Main.main(new String[]{
                "-debug",
                "-prepend-classpath",
                /////////"-cp", "/Users/burcukulahciogluozkan/EvSerChecker/asyncRob/ser/instrumentor",
                //"-process-dir", "/Users/burcukulahciogluozkan/EvSerChecker/apks",

                //"-process-dir", "/Users/burcukulahciogluozkan/EvSerChecker/apks/sample_vlillechecker.apk",
                //"-android-jars", "/Users/burcukulahciogluozkan/adt-bundle/sdk/platforms",
                "-process-dir", args[0],
                "-android-jars", args[1],
                "-src-prec", "apk",
                "-output-format", "dex",
                "-allow-phantom-refs"
        });
    }

    public AsyncCounterBodyTransformer(String packName) {
        packageName = packName;
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

    }

    @Override
    protected void internalTransform(final Body b, String phaseName,
                                     @SuppressWarnings("rawtypes") Map options) {

        initTransformer();

        String className = b.getMethod().getDeclaringClass().toString();
        String methodName = b.getMethod().getName();
        SootClass clazz = b.getMethod().getDeclaringClass();

        if(!(className.startsWith(packageName)) )
            return;


        // instrument event handlers, lifecycle event handlers, posted runnables and message handlers
        if(isEventHandler(methodName, clazz)  || isLifeCycleEventHandler(methodName, clazz)
                || (methodName.equals("run") && clazz.implementsInterface("java.lang.Runnable")) || (methodName.equals("handleMessage") && SootUtils.hasParentClass(clazz, handlerClass)))
            instrumentEventHandlerMethod(b, methodName, className);

        instrumentMethod(b, methodName, className);

    }

    private boolean isEventHandler(String methodName, SootClass clazz) {

        SootClass viewClass = Scene.v().getSootClass("android.view.View");

        if (methodName.startsWith("on") && SootUtils.hasParentClass(clazz, viewClass)) {
            System.out.println("Event handler of a View: " + methodName);
            return true;
        }

        if (methodName.startsWith("on") && clazz.getName().endsWith("Listener")) {
            System.out.println("Method of a listener: " + methodName);
            return true;
        }

        if (methodName.startsWith("on")) {
            Chain<SootClass> cc = clazz.getInterfaces();

            for (SootClass sc : cc) {
                // if no second check, Activity that implements a listener -> onCreate as an event
                // third check - Listeners onXXX defined in the app itself are synchronously executed
                if (sc.getName().endsWith("Listener") && sc.declaresMethodByName(methodName) && !sc.getName().startsWith(packageName)) {
                    System.out.println("Interface listener: " + methodName + " " + sc.getName());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isLifeCycleEventHandler(String methodName, SootClass clazz) {

        SootClass activityClass = Scene.v().getSootClass("android.app.Activity");
        SootClass fragmentClass = Scene.v().getSootClass("android.app.Fragment");
        SootClass supportFragmentClass = Scene.v().getSootClass("android.support.v4.app.ListFragment");
        SootClass supportFragmentListClass = Scene.v().getSootClass("android.support.v4.app.ListFragment");

        if (methodName.startsWith("on") && SootUtils.hasParentClass(clazz, activityClass)) {
            System.out.println("Lifecycle event handler of an Activity: " + methodName);
            return true;
        }

        if (methodName.startsWith("on") && (SootUtils.hasParentClass(clazz, fragmentClass)
                || SootUtils.hasParentClass(clazz, supportFragmentClass) || SootUtils.hasParentClass(clazz, supportFragmentListClass))) {
            System.out.println("Lifecycle event handler of a Fragment: " + methodName);
            return true;
        }

        return false;
    }

    // set id in the beginning, reset at the end
    public void instrumentEventHandlerMethod(final Body b, final String methodName, final String className) {
        final PatchingChain<Unit> units = b.getUnits();
        Iterator<Unit> iter = units.snapshotIterator();

        if(hasReturningTrap(b)) {
            System.out.println("=================Not instrumented method: " + methodName + " " + className);
            System.out.println("=================It ends with a try/catch block");
            return;
        }

        Stmt stmt = ((JimpleBody) b).getFirstNonIdentityStmt();

        // save the current event id before executing that event handler
        // i.e. 0 if not in event handler, >0 if already in event handler
        final Local oldEventId = SootUtils.addTmpPrimitiveInt(b, "async_OldEventId");
        InvokeExpr expr = Jimple.v().newStaticInvokeExpr(getCurrentEventIdMethod.makeRef());
        units.insertBefore(Jimple.v().newAssignStmt(oldEventId, expr), stmt);

        // check and set new event id to the main thread
        final Local methodNameLocal = SootUtils.addTmpString(b, "async_methNameStr");
        units.insertBefore(Jimple.v().newAssignStmt(methodNameLocal, StringConstant.v(methodName)), stmt);
        final Local classNameLocal = SootUtils.addTmpString(b, "async_classNameStr");
        units.insertBefore(Jimple.v().newAssignStmt(classNameLocal, StringConstant.v(className)), stmt);
        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(checkAndSetEventIdMethod.makeRef(), methodNameLocal, classNameLocal)), stmt);

        //System.out.println("===========Event handler stmt to assign event id added for: " + methodName);

        // restore the old event id before returning
        while (iter.hasNext()) {

            iter.next().apply(new AbstractStmtSwitch() {

                public void caseReturnVoidStmt(ReturnVoidStmt stmt) {
                    // set event id of the main thread to the "eventId" at the beginning of the method
                    units.insertBefore(SootUtils.staticInvocation(setCurrentEventIdMethod, oldEventId, methodNameLocal, classNameLocal), stmt);
                    System.out.println("===========Event handler reset stmt added.. " + methodName + " " + className);
                }

                public void caseReturnStmt(ReturnStmt stmt) {
                    // set event id of the main thread to the "eventId" at the beginning of the method
                    units.insertBefore(SootUtils.staticInvocation(setCurrentEventIdMethod, oldEventId, methodNameLocal, classNameLocal), stmt);
                    System.out.println("===========Event handler reset stmt added.. " + methodName + " " + className);
                }

                public void caseRetStmt(RetStmt stmt) {
                    // set event id of the main thread to the "eventId" at the beginning of the method
                    units.insertBefore(SootUtils.staticInvocation(setCurrentEventIdMethod, oldEventId, methodNameLocal, classNameLocal), stmt);
                    System.out.println("===========Event handler reset stmt added.. " + methodName + " " + className);
                }
            });
        }
    }

    // increment the total number of asynctasks of the current event
    public void instrumentMethod(final Body b, final String methodName, final String className) {
        final PatchingChain<Unit> units = b.getUnits();
        Iterator<Unit> iter = units.snapshotIterator();

        while (iter.hasNext()) {
            iter.next().apply(new AbstractStmtSwitch() {

                public void caseInvokeStmt(InvokeStmt stmt) {
                    String invokedMethodName = stmt.getInvokeExpr().getMethod().getName();
                    SootClass invokedMethodClass = stmt.getInvokeExpr().getMethod().getDeclaringClass();
                    String invokedMethodClassName = invokedMethodClass.getName();

                    if ((invokedMethodName.equals("execute") || invokedMethodName.equals("executeOnExecutor")) && invokedMethodClassName.equals("android.os.AsyncTask")) {
                        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(incrementAsyncTaskMethod.makeRef())), stmt);
                        System.out.println("===========AsyncTask increment stmt added in: " + methodName + " of " + className);
                    } else if (invokedMethodName.contains("sendMessage") && SootUtils.hasParentClass(invokedMethodClass, handlerClass)) {
                        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(incrementMsgTaskMethod.makeRef())), stmt);
                        System.out.println("===========Message increment stmt added in: " + methodName + " of " + className);
                    } else if (invokedMethodName.contains("post") && SootUtils.hasParentClass(invokedMethodClass, handlerClass)) {
                        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(incrementMsgTaskMethod.makeRef())), stmt);
                        System.out.println("===========Message increment stmt added in: " + methodName + " of " + className);
                    } else if (invokedMethodName.contains("sendToTarget") && SootUtils.hasParentClass(invokedMethodClass, messageClass)) {
                        units.insertBefore(Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(incrementMsgTaskMethod.makeRef())), stmt);
                        System.out.println("===========Message increment stmt added in: " + methodName + " of " + className);
                    }
                }
            });
        }

    }

    public boolean hasReturningTrap(Body b) {
        Chain<Trap> traps = b.getTraps();

        for(Trap t:traps) {
            Unit endUnit = t.getEndUnit();
            //System.out.println("=================End unit: " + endUnit.toString());
            if(endUnit.toString().contains("return")) {
                return true;
            }
        }

        return false;
    }
}
