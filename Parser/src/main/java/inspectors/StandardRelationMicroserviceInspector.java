package inspectors;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.AllArgsConstructor;
import managers.ProjectClassesManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StandardRelationMicroserviceInspector {
    public static HashMap<String, Integer> processClass(String microserviceName, CtClass clazz, ProjectClassesManager projectClassesManager) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        HashMap<String, Integer> relations = new HashMap<>();

        for (CtMethod method : methods) {
            try {
                method.instrument(new StandardMicroserviceRelationBuilder(microserviceName, clazz, method, projectClassesManager, relations));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return relations;
    }
}

@AllArgsConstructor
class StandardMicroserviceRelationBuilder extends ExprEditor {
    private final String inMicroserviceName;
    private final CtClass callerClass;
    private final CtMethod callerMethod;

    private final ProjectClassesManager projectClassesManager;

    private final HashMap<String, Integer> relations;

    @Override
    public void edit(MethodCall m) {
        readRelation(m.getClassName(), m.getMethodName(), m.getSignature());
    }

    public void edit(ConstructorCall c) {
        readRelation(c.getClassName(), c.getMethodName(), c.getSignature());
    }

    public void readRelation(String className, String methodName, String methodSignature) {
        StringBuilder calledMethodSignature = new StringBuilder(methodName + "(");
        StringBuilder callerMethodSignature = new StringBuilder(callerMethod.getName() + "(");
        ClassInspector.getMethodSignature(calledMethodSignature, methodSignature);
        ClassInspector.getMethodSignature(callerMethodSignature, callerMethod.getSignature());

        String outMicroserviceName = projectClassesManager.getClassMicroserviceName(className);

        if(outMicroserviceName != null && !inMicroserviceName.equals(outMicroserviceName)) {
            String info = inMicroserviceName + "||" + callerClass.getName() + "||" + outMicroserviceName + "||" + className + "||" + callerMethodSignature + "||" + calledMethodSignature;
            if (!relations.containsKey(info)) {
                relations.put(info, 1);
            } else {
                relations.put(info, relations.get(info) + 1);
            }
        }
    }
}
