package inspectors;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import utility.Utility;

import java.util.*;

public class FeignRelationMicroserviceInspector {

    public static final HashMap<String, Endpoint> endpoints = new HashMap<>();
    public static final HashMap<String, FeignRelation> relations = new HashMap<>();

    public static void processClass(String microserviceName, CtClass ctClass) {
        if (Utility.isApiClass(ctClass)) {
            processApiClass(microserviceName, ctClass);
        }
        if (Utility.isFeignClass(ctClass)) {
            processFeignClass(microserviceName, ctClass);
        }
    }

    public static HashMap<String, Integer> getMicroserviceRelationsInfo(String microserviceName, CtClass clazz) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        methods.addAll(List.of(clazz.getMethods()));
        HashMap<String, Integer> microserviceRelationsInfo = new HashMap<>();

        for (CtMethod method : methods) {
            try {
                method.instrument(new MicroserviceRelationBuilder(microserviceName, clazz, method, microserviceRelationsInfo));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return microserviceRelationsInfo;
    }

    public static void reset() {
        endpoints.clear();
        relations.clear();
    }

    private static void processApiClass(String microserviceName, CtClass clazz) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));
        methods.addAll(List.of(clazz.getMethods()));

        for (CtMethod method : methods) {
            if(Utility.isApiFunction(method)) {
                StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
                ClassInspector.getMethodSignature(methodSignature, method.getSignature());

                Endpoint endpoint = Endpoint.builder()
                        .microServiceName(microserviceName)
                        .className(clazz.getName())
                        .methodSignature(methodSignature.toString())
                        .path(Utility.getPath(method, clazz))
                        .httpMethod(Utility.getHttpMethod(method))
                        .build();
                endpoints.put(endpoint.getMicroServiceName() + "." + endpoint.getPath() + "." + endpoint.getHttpMethod(), endpoint);
            }
        }
    }

    private static void processFeignClass(String microserviceName, CtClass clazz) {
        Set<CtMethod> methods = new HashSet<>(List.of(clazz.getDeclaredMethods()));

        for (CtMethod method : methods) {
            if(Utility.isApiFunction(method)) {
                StringBuilder methodSignature = new StringBuilder(method.getName() + "(");
                ClassInspector.getMethodSignature(methodSignature, method.getSignature());

                FeignRelation feignRelation = FeignRelation.builder()
                        .inMicroServiceName(microserviceName)
                        .outMicroServiceName(Utility.getFeignClientName(clazz))
                        .className(clazz.getName())
                        .methodSignature(methodSignature.toString())
                        .path(Utility.getPath(method, clazz))
                        .httpMethod(Utility.getHttpMethod(method))
                        .build();
                relations.put(feignRelation.getInMicroServiceName() + "." + feignRelation.getClassName() + "." + feignRelation.getMethodSignature(), feignRelation);
            }
        }
    }
}

@AllArgsConstructor
class MicroserviceRelationBuilder extends ExprEditor {
    private final String inMicroserviceName;
    private final CtClass callerClass;
    private final CtMethod callerMethod;
    private final HashMap<String, Integer> microserviceRelationsInfo;

    @Override
    public void edit(MethodCall m) {
        StringBuilder calledMethodSignature = new StringBuilder(m.getMethodName() + "(");
        StringBuilder callerMethodSignature = new StringBuilder(callerMethod.getName() + "(");
        ClassInspector.getMethodSignature(calledMethodSignature, m.getSignature());
        ClassInspector.getMethodSignature(callerMethodSignature, callerMethod.getSignature());

        String feignRelationKey = inMicroserviceName + "." + m.getClassName() + "." + calledMethodSignature;

        if (FeignRelationMicroserviceInspector.relations.containsKey(feignRelationKey)) {
            FeignRelation relation = FeignRelationMicroserviceInspector.relations.get(feignRelationKey);
            Endpoint endpoint = FeignRelationMicroserviceInspector.endpoints.get(relation.getOutMicroServiceName() + "." + relation.getPath() + "." + relation.getHttpMethod());
            if(endpoint != null) {
                String info = inMicroserviceName + "||" + callerClass.getName() + "||" + relation.getOutMicroServiceName() + "||" + endpoint.getClassName() + "||" + relation.getMethodSignature() + "||" + endpoint.getMethodSignature();
                if (microserviceRelationsInfo.containsKey(info)) {
                    microserviceRelationsInfo.put(info, microserviceRelationsInfo.get(info) + 1);
                } else {
                    microserviceRelationsInfo.put(info, 1);
                }
            }
        }
    }
}

@Getter
@AllArgsConstructor
@Builder
@ToString
class Endpoint {
    private final String microServiceName;
    private final String className;
    private final String methodSignature;
    private final String path;
    private final String httpMethod;
}


@Getter
@AllArgsConstructor
@Builder
@ToString
class FeignRelation {
    private final String inMicroServiceName;
    private final String outMicroServiceName;
    private final String className;
    private final String methodSignature;
    private final String path;
    private final String httpMethod;
}
