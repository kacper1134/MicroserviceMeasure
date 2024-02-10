import javassist.CtMethod;
import lombok.SneakyThrows;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javassist.CtClass;

import java.util.Arrays;

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

    private static boolean hasAnnotation(CtClass ctClass, Class<?> annotationClass) {
        return ctClass.hasAnnotation(annotationClass);
    }
}