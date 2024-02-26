import javassist.*;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import lombok.AllArgsConstructor;

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

    public static ClassRelationTypesHolder getMethodRelationsInfo(CtClass clazz, MicroserviceClassesManager manager) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        methods.addAll(List.of(clazz.getMethods()));
        ClassRelationTypesHolder holder = new ClassRelationTypesHolder();

        for (CtMethod method : methods) {
            try {
                method.instrument(new ClassRelationBuilder(clazz, method, manager, holder));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return holder;
    }

    public static FieldRelationTypesHolder getFieldRelationsInfo(CtClass clazz, MicroserviceClassesManager manager) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        methods.addAll(List.of(clazz.getMethods()));
        FieldRelationTypesHolder holder = new FieldRelationTypesHolder();

        for (CtMethod method : methods) {
            try {
                method.instrument(new FieldRelationBuilder(clazz, method, manager, holder));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return holder;
    }

    public static String getNumberOfLines(CtClass clazz, MicroserviceClassesManager manager) {
        String filePath = manager.getPathToClassFile(clazz.getName());
        String classContent = ClassDecompiler.decompile(filePath);
        String classLines =  clazz.getName() + "||" + ClassFileReader.getNumberOfLinesOfClass(classContent, clazz.getSimpleName());

        StringBuilder result = new StringBuilder(classLines + "\n");

        for (CtMethod method: clazz.getDeclaredMethods()) {
            StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
            ClassInspector.getMethodSignature(methodSignature, method.getSignature());
            int numberOfLines = ClassFileReader.getNumberOfLinesOfMethod(classContent, clazz.getSimpleName(), methodSignature.toString());

            result.append(clazz.getName()).append("||").append(methodSignature).append("||").append(numberOfLines).append("\n");
        }

        for (CtConstructor constructor: clazz.getDeclaredConstructors()) {
            StringBuilder methodSignature = new StringBuilder(constructor.getName() + "(");
            ClassInspector.getMethodSignature(methodSignature, constructor.getSignature());
            int numberOfLines = ClassFileReader.getNumberOfLinesOfMethod(classContent, clazz.getSimpleName(), methodSignature.toString());

            result.append(clazz.getName()).append("||").append(methodSignature).append("||").append(numberOfLines).append("\n");
        }

        return result.toString();
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

@AllArgsConstructor
class ClassRelationBuilder extends ExprEditor {
    private final CtClass callerClass;
    private final CtMethod callerMethod;

    private final MicroserviceClassesManager manager;

    private final ClassRelationTypesHolder holder;

    @Override
    public void edit(MethodCall m) {
        StringBuilder calledMethodSignature = new StringBuilder(m.getMethodName() + "(");
        StringBuilder callerMethodSignature = new StringBuilder(callerMethod.getName() + "(");
        ClassInspector.getMethodSignature(calledMethodSignature, m.getSignature());
        ClassInspector.getMethodSignature(callerMethodSignature, callerMethod.getSignature());
        if (manager.isClassDeclared(m.getClassName())) {
            CtClass calledClass = manager.getClass(m.getClassName());
            boolean isCallerClassApi = Utility.isApiClass(callerClass);
            boolean isCalledClassApi = Utility.isApiClass(calledClass);
            String info = callerClass.getName() + "||" + callerMethodSignature + "||" + calledClass.getName() + "||" + calledMethodSignature;

            if(isCalledClassApi && isCallerClassApi) {
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

@AllArgsConstructor
class FieldRelationBuilder extends ExprEditor {
    private final CtClass callerClass;
    private final CtMethod callerMethod;
    private final MicroserviceClassesManager manager;
    private final FieldRelationTypesHolder holder;

    @Override
    public void edit(FieldAccess f) {
        String fieldName = f.getFieldName();
        String calledClassName = f.getClassName();

        StringBuilder callerMethodSignature = new StringBuilder(callerMethod.getName() + "(");
        ClassInspector.getMethodSignature(callerMethodSignature, callerMethod.getSignature());

        if (manager.isClassDeclared(calledClassName)) {
            boolean isCallerClassApi = Utility.isApiClass(callerClass);
            String info = callerClass.getName() + "||" + callerMethodSignature + "||" + calledClassName + "||" + fieldName;

            boolean isRecursive = callerClass.getName().equals(calledClassName);

            if(isRecursive) return;

            if(isCallerClassApi) {
                if (holder.fieldInterfaceRelation.containsKey(info))
                    holder.fieldInterfaceRelation.put(info, holder.fieldInterfaceRelation.get(info) + 1);
                else
                    holder.fieldInterfaceRelation.put(info, 1);
            } else {
                if (holder.fieldClassRelation.containsKey(info))
                    holder.fieldClassRelation.put(info, holder.fieldClassRelation.get(info) + 1);
                else
                    holder.fieldClassRelation.put(info, 1);
            }
        }
    }
}

class ClassRelationTypesHolder {
    public final HashMap<String, Integer> classToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> classToInterfaceRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToClassRelation = new HashMap<>();
    public final HashMap<String, Integer> interfaceToInterfaceRelation = new HashMap<>();
}

class FieldRelationTypesHolder {
    public final HashMap<String, Integer> fieldClassRelation = new HashMap<>();
    public final HashMap<String, Integer> fieldInterfaceRelation = new HashMap<>();
}