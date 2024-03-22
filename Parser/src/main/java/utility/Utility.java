package utility;

import fileReaders.JavaFileReader;
import javassist.CannotCompileException;
import javassist.CtMethod;
import lombok.SneakyThrows;
import managers.MicroserviceClassesManager;
import managers.ProjectClassesManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javassist.CtClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Utility {
    public static boolean isApiClass(CtClass ctClass) {
        if (ctClass == null) {
            return false;
        }

        return hasAnnotation(ctClass, Controller.class) ||
                hasAnnotation(ctClass, RestController.class) ||
                hasAnnotation(ctClass, ResponseBody.class) ||
                hasAnnotation(ctClass, RequestMapping.class);
    }

    public static boolean isFeignClass(CtClass ctClass) {
        if (ctClass == null) {
            return false;
        }

        return hasAnnotation(ctClass, FeignClient.class);
    }

    public static boolean isApiFunction(CtMethod ctMethod) {
        if (ctMethod == null) {
            return false;
        }

        return ctMethod.hasAnnotation(GetMapping.class) ||
                ctMethod.hasAnnotation(PostMapping.class) ||
                ctMethod.hasAnnotation(PutMapping.class) ||
                ctMethod.hasAnnotation(DeleteMapping.class) ||
                ctMethod.hasAnnotation(PatchMapping.class) ||
                ctMethod.hasAnnotation(RequestMapping.class);
    }

    public static boolean isKafkaConsumerFunction(CtMethod ctMethod) {
        return ctMethod.hasAnnotation(KafkaListener.class);
    }

    public static boolean isKafkaConsumerClass(CtClass ctClass) {
        return ctClass.hasAnnotation(KafkaListener.class);
    }

    public static boolean isKafkaHandlerFunction(CtMethod ctMethod) {
        return ctMethod.hasAnnotation(KafkaHandler.class);
    }

    @SneakyThrows
    public static String getHttpMethod(CtMethod ctMethod) {
        if (ctMethod.hasAnnotation(GetMapping.class)) {
            return "GET";
        } else if (ctMethod.hasAnnotation(PostMapping.class)) {
            return "POST";
        } else if (ctMethod.hasAnnotation(PutMapping.class)) {
            return "PUT";
        } else if (ctMethod.hasAnnotation(DeleteMapping.class)) {
            return "DELETE";
        } else if (ctMethod.hasAnnotation(PatchMapping.class)) {
            return "PATCH";
        } else if (ctMethod.hasAnnotation(RequestMapping.class)) {
            RequestMapping requestMappingAnnotation = (RequestMapping) ctMethod.getAnnotation(RequestMapping.class);
            RequestMethod[] methods = requestMappingAnnotation.method();
            if (methods.length > 0) {
                return methods[0].toString();
            }
        }
        return "";
    }

    @SneakyThrows
    public static String getPath(CtMethod ctMethod, CtClass ctClass) {
        String path = "";

        if (ctClass.hasAnnotation(RequestMapping.class)) {
            RequestMapping requestMapping = (RequestMapping) ctClass.getAnnotation(RequestMapping.class);
            if(requestMapping.value().length > 0)
                path = requestMapping.value()[0];
        }
        if (ctMethod.hasAnnotation(GetMapping.class)) {
            GetMapping getMapping = (GetMapping) ctMethod.getAnnotation(GetMapping.class);
            if(getMapping.value().length > 0)
                return path + getMapping.value()[0];
        } else if (ctMethod.hasAnnotation(PostMapping.class)) {
            PostMapping postMapping = (PostMapping) ctMethod.getAnnotation(PostMapping.class);
            if(postMapping.value().length > 0)
                return path + postMapping.value()[0];
        } else if (ctMethod.hasAnnotation(PutMapping.class)) {
            PutMapping putMapping = (PutMapping) ctMethod.getAnnotation(PutMapping.class);
            if(putMapping.value().length > 0)
                return path + putMapping.value()[0];
        } else if (ctMethod.hasAnnotation(DeleteMapping.class)) {
            DeleteMapping deleteMapping = (DeleteMapping) ctMethod.getAnnotation(DeleteMapping.class);
            if(deleteMapping.value().length > 0)
                return path + deleteMapping.value()[0];
        } else if (ctMethod.hasAnnotation(PatchMapping.class)) {
            PatchMapping patchMapping = (PatchMapping) ctMethod.getAnnotation(PatchMapping.class);
            if(patchMapping.value().length > 0)
                return path + patchMapping.value()[0];
        } else if (ctMethod.hasAnnotation(RequestMapping.class)) {
            RequestMapping requestMapping = (RequestMapping) ctMethod.getAnnotation(RequestMapping.class);
            if(requestMapping.value().length > 0)
                return path + requestMapping.value()[0];
        }
        return path;
    }

    @SneakyThrows
    public static String getFeignClientName(CtClass ctClass) {
        FeignClient feignClient = (FeignClient) ctClass.getAnnotation(FeignClient.class);
        return feignClient.value();
    }

    @SneakyThrows
    public static String getFeignClassPathFromClientName(CtClass ctClass) {
        FeignClient feignClient = (FeignClient) ctClass.getAnnotation(FeignClient.class);
        String name = feignClient.value();
        if (name.isEmpty()) {
            name = feignClient.name();
        }
        if (!name.contains("/")) {
            return "";
        }
        name = name.strip();
        return name.substring(name.indexOf("/") + 1);
    }

    @SneakyThrows
    public static String getApiClassPathFromRequestMapping(CtClass ctClass) {
        RequestMapping requestMapping = (RequestMapping) ctClass.getAnnotation(RequestMapping.class);
        if (requestMapping == null) {
            return "";
        }
        if (requestMapping.path().length == 0) {
            return "";
        }
        return requestMapping.path()[0];
    }

    @SneakyThrows
    public static ArrayList<String> getKafkaTopics(CtMethod ctMethod) {
        KafkaListener kafkaListener = (KafkaListener) ctMethod.getAnnotation(KafkaListener.class);
        if(kafkaListener != null) {
            return new ArrayList<>(Arrays.stream(kafkaListener.topics()).toList());
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    public static ArrayList<String> getKafkaTopics(CtClass ctClass) {
        KafkaListener kafkaListener = (KafkaListener) ctClass.getAnnotation(KafkaListener.class);
        if(kafkaListener != null) {
            return new ArrayList<>(Arrays.stream(kafkaListener.topics()).toList());
        }
        return new ArrayList<>();
    }

    @SneakyThrows
    public static Set<CtMethod> transformCtConstructorsToCtMethods(CtClass clazz) {
        return Arrays.stream(clazz.getDeclaredConstructors()).map(c -> {
            try {
                CtMethod method = CtMethod.make(c.getMethodInfo(), clazz);
                method.setName(c.getName());
                return method;
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
    }

    @SneakyThrows
    public static String extractKafkaTopicFromKafkaInstruction(String filePath, String instruction, MicroserviceClassesManager manager, String fullCurrentClassName, ProjectClassesManager projectClassesManager) {
        String topicParameterValue = extractTopicParameterValueFromInstruction(instruction);

        if (topicParameterValue == null) {
            return null;
        }

        if (isStringLiteral(topicParameterValue)) {
            return topicParameterValue.substring(1, topicParameterValue.length() - 1);
        }

        String simpleClassName = extractClassNameFromTopicParameter(topicParameterValue);
        String fullClassName = fullCurrentClassName;

        String fieldName = extractFieldNameFromTopicParameter(topicParameterValue);
        if (simpleClassName != null) {
            fullClassName = JavaFileReader.getFullClassNameFromImportStatement(simpleClassName, filePath);
        }

        Value fieldValue = null;
        try {
            fieldValue = (Value) projectClassesManager.getClass(fullClassName).getField(fieldName).getAnnotation(Value.class);
        }
        catch (Exception ignored){
        }
        if (fieldValue != null) {
            return fieldValue.value();
        }


        topicParameterValue = JavaFileReader.getValueOfField(manager.getPathToJavaFile(fullClassName), fieldName);

        ArrayList<String> staticImportClasses = JavaFileReader.getStaticImportClasses(manager.getPathToJavaFile(fullClassName));

        for (String staticImportClass : staticImportClasses) {
            topicParameterValue = JavaFileReader.getValueOfField(manager.getPathToJavaFile(staticImportClass), fieldName);
            if (topicParameterValue != null) {
                break;
            }
        }

        if (topicParameterValue != null && isStringLiteral(topicParameterValue)) {
            return topicParameterValue.substring(1, topicParameterValue.length() - 1);
        }

        return null;
    }

    private static String extractTopicParameterValueFromInstruction(String instruction) {
        Pattern sendPattern = Pattern.compile("\\.send\\(\\s*([^,\\s]+)");
        Pattern receivePattern = Pattern.compile("\\.receive\\(\\s*([^,\\s]+)");
        Pattern streamPattern = Pattern.compile("\\.stream\\(\\s*([^,\\s]+)");

        Matcher sendMatcher = sendPattern.matcher(instruction);
        Matcher receiveMatcher = receivePattern.matcher(instruction);
        Matcher streamMatcher = streamPattern.matcher(instruction);

        if (sendMatcher.find()) {
            return sendMatcher.group(1).trim().replace("\")", "\"");
        }
        if (receiveMatcher.find()) {
            return receiveMatcher.group(1).trim().replace("\")", "\"");
        }
        if (streamMatcher.find()) {
            return streamMatcher.group(1).trim().replace("\")", "\"");
        }
        return null;
    }

    private static String extractClassNameFromTopicParameter(String topicParameter) {
        Pattern pattern = Pattern.compile("([\\w$]+)\\.");
        Matcher matcher = pattern.matcher(topicParameter);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static String extractFieldNameFromTopicParameter(String topicParameter) {
        Pattern pattern = Pattern.compile("(?:[\\w$]+\\.)?(\\w+)(?:\\.\\(\\))?");
        Matcher matcher = pattern.matcher(topicParameter);

        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static boolean isStringLiteral(String value) {
        return value.startsWith("\"") && value.endsWith("\"");
    }

    private static boolean hasAnnotation(CtClass ctClass, Class<?> annotationClass) {
        return ctClass.hasAnnotation(annotationClass);
    }
}