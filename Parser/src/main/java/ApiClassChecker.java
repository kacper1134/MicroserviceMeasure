import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javassist.CtClass;

public class ApiClassChecker {
    public static boolean isApiClass(CtClass ctClass) {
        if (ctClass == null) {
            return false;
        }

        return hasAnnotation(ctClass, Controller.class) ||
                hasAnnotation(ctClass, RestController.class) ||
                hasAnnotation(ctClass, ResponseBody.class) ||
                hasAnnotation(ctClass, RequestMapping.class);
    }

    private static boolean hasAnnotation(CtClass ctClass, Class<?> annotationClass) {
        return ctClass.hasAnnotation(annotationClass);
    }
}