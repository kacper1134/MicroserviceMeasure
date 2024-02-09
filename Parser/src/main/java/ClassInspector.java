import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.util.*;

public class ClassInspector {
    public static ArrayList<String> getMethodsInfo(CtClass clazz, boolean isApiClass) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        if(!isApiClass)
            methods.addAll(List.of(clazz.getMethods()));
        ArrayList<String> classMethodsInfo = new ArrayList<>();

        for (CtMethod method : methods) {
            String methodName = method.getName();
            StringBuilder methodSignature = new StringBuilder(methodName + "(");
            getMethodSignature(methodSignature, method.getSignature());

            String methodModifier = "";
            int modifiers = method.getModifiers();
            if (java.lang.reflect.Modifier.isPublic(modifiers)) {
                methodModifier = "public";
            }
            else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
                if(isApiClass) continue;
                methodModifier = "protected";
            }
            else if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
                if(isApiClass) continue;
                methodModifier = "private";
            }

            String returnType;
            try {
                returnType = method.getReturnType().getName();
            } catch (NotFoundException e) {
                returnType = e.getMessage();
            }

            String methodInfo = clazz.getName() + "||" + methodSignature + "||" + methodModifier + "||" + returnType;
            classMethodsInfo.add(methodInfo);
        }
        return classMethodsInfo;
    }

    public static ArrayList<String> getFieldsInfo(CtClass clazz) {
        Set<CtField> fields = new HashSet<>(List.of(clazz.getDeclaredFields()));
        fields.addAll(List.of(clazz.getFields()));

        ArrayList<String> classFieldsInfo = new ArrayList<>();

        for (CtField field : fields) {
            String fieldName = field.getName();
            String fieldType;

            try {
                fieldType = field.getType().getName();
            } catch (NotFoundException e) {
                fieldType = e.getMessage();
            }

            String fieldModifier = "";
            int modifiers = field.getModifiers();
            if (java.lang.reflect.Modifier.isPublic(modifiers)) {
                fieldModifier = "public";
            }
            else if (java.lang.reflect.Modifier.isProtected(modifiers)) {
                fieldModifier = "protected";
            }
            else if (java.lang.reflect.Modifier.isPrivate(modifiers)) {
                fieldModifier = "private";
            }
            String fieldInfo = clazz.getName() + "||" + fieldName + "||" + fieldModifier + "||" + fieldType;

            classFieldsInfo.add(fieldInfo);
        }

        return classFieldsInfo;
    }

    public static RelationTypesHolder getMethodRelationsInfo(CtClass clazz, MicroserviceClassesManager manager) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        methods.addAll(List.of(clazz.getMethods()));
        RelationTypesHolder holder = new RelationTypesHolder();

        for (CtMethod method : methods) {
            try {
                method.instrument(new RelationBuilder(clazz, method, manager, holder));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return holder;
    }

    public static void getMethodSignature(StringBuilder methodSignature, String signature) {
        String paramStr = Descriptor.toString(signature);
        paramStr = paramStr.substring(1, paramStr.length() - 1);
        String[] paramSplit = paramStr.split(",");

        for (int i = 0; i < paramSplit.length; i++) {
            if (i > 0) {
                methodSignature.append(", ");
            }
            methodSignature.append(paramSplit[i]);
        }
        methodSignature.append(")");
    }
}

class RelationBuilder extends ExprEditor {
    private final CtClass callerClass;
    private final CtMethod callerMethod;

    private final MicroserviceClassesManager manager;

    private final RelationTypesHolder holder;

    public RelationBuilder(CtClass callerClass, CtMethod callerMethod, MicroserviceClassesManager manager, RelationTypesHolder holder) {
        this.callerClass = callerClass;
        this.callerMethod = callerMethod;
        this.manager = manager;
        this.holder = holder;
    }

    @Override
    public void edit(MethodCall m) {
        StringBuilder calledMethodSignature = new StringBuilder(m.getMethodName() + "(");
        StringBuilder callerMethodSignature = new StringBuilder(callerMethod.getName() + "(");
        ClassInspector.getMethodSignature(calledMethodSignature, m.getSignature());
        ClassInspector.getMethodSignature(callerMethodSignature, callerMethod.getSignature());
        if (manager.isClassDeclared(m.getClassName())) {
            CtClass calledClass = manager.getClass(m.getClassName());
            boolean isCallerClassApi = ApiClassChecker.isApiClass(callerClass);
            boolean isCalledClassApi = ApiClassChecker.isApiClass(calledClass);
            String info = callerClass.getName() + "||" + callerMethodSignature + "||" + calledClass.getName() + "||" + calledMethodSignature;

            boolean isRecursive = callerClass.getName().equals(calledClass.getName());

            if(isCalledClassApi && isCallerClassApi) {
                if(isRecursive) return;
                if (holder.interfaceToInterfaceRelation.containsKey(info))
                    holder.interfaceToInterfaceRelation.put(info, holder.interfaceToInterfaceRelation.get(info) + 1);
                else
                    holder.interfaceToInterfaceRelation.put(info, 1);
            } else if(isCalledClassApi) {
                if (holder.classToInterfaceRelation.containsKey(info))
                    holder.classToInterfaceRelation.put(info, holder.classToInterfaceRelation.get(info) + 1);
                else
                    holder.classToInterfaceRelation.put(info, 1);
            } else if(isCallerClassApi) {
                if (holder.interfaceToClassRelation.containsKey(info))
                    holder.interfaceToClassRelation.put(info, holder.interfaceToClassRelation.get(info) + 1);
                else
                    holder.interfaceToClassRelation.put(info, 1);
            } else {
                if (holder.classToClassRelation.containsKey(info))
                    holder.classToClassRelation.put(info, holder.classToClassRelation.get(info) + 1);
                else
                    holder.classToClassRelation.put(info, 1);
            }
        }
    }
}


class RelationTypesHolder {
    public final HashMap<String, Integer> classToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> classToInterfaceRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToInterfaceRelation = new HashMap<>();
}
