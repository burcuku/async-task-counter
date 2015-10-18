package instrumentor;

import soot.*;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;

public class SootUtils {

    public static InvokeStmt specialInvocation(SootMethod m, Local tmpRef, Value... args) {
        SootMethod toCall = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
        return Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tmpRef, toCall.makeRef(), args));
    }

    public static InvokeStmt staticInvocation(SootMethod m, Value... args) {
        return Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(m.makeRef(), args));
    }
    
    public static InvokeStmt staticInvocation(SootMethod m) {
        return Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(m.makeRef()));
    }

    public static InvokeStmt staticInvocation(SootMethod m, Local arg) {
        return Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(m.makeRef(),arg));
    }
    
    public static boolean hasParentClass(SootClass clazz, SootClass ancestor) {
        if(clazz == ancestor)
            return true;
        if(clazz.getName().equalsIgnoreCase("java.lang.Object"))
            return false;

        return hasParentClass(clazz.getSuperclass(), ancestor);
    }

    public static Local addTmpRef(Body body, String s)
    {
        Local tmpRef = Jimple.v().newLocal(s, RefType.v("java.io.PrintStream"));
        body.getLocals().add(tmpRef);
        return tmpRef;
    }

    public static Local addTmpBool(Body body, String s)
    {
        Local tmpBool = Jimple.v().newLocal(s, BooleanType.v());
        body.getLocals().add(tmpBool);
        return tmpBool;
    }

    public static Local addTmpString(Body body, String s)
    {
        Local tmpString = Jimple.v().newLocal(s, RefType.v("java.lang.String"));
        body.getLocals().add(tmpString);
        return tmpString;
    }

    public static Local addTmpPrimitiveInt(Body body, String s)
    {
        Local tmpInt =  Jimple.v().newLocal(s, IntType.v());
        body.getLocals().add(tmpInt);
        return tmpInt;
    }

    public static Local addTmpInteger(Body body, String s)
    {
        Local tmpInt =  Jimple.v().newLocal(s, RefType.v("java.lang.Integer"));
        body.getLocals().add(tmpInt);
        return tmpInt;
    }
}
