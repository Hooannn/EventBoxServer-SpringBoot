package com.ht.eventbox.annotations;

import com.ht.eventbox.config.HttpException;
import com.ht.eventbox.constant.Constant;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.List;

@Aspect
@Component
public class PermissionsAspect {

    @Around("@annotation(requiredPermissions)")
    public Object checkRole(ProceedingJoinPoint pjp, RequiredPermissions requiredPermissions) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        List<String> userPermissions = (List<String>) requestAttributes.getAttribute("permissions", RequestAttributes.SCOPE_REQUEST);

        if (userPermissions == null || userPermissions.isEmpty()) {
            throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
        }

        for (String requiredPermission : requiredPermissions.value()) {
            boolean pass = false;

            String[] requiredParts = requiredPermission.split(":");
            String requiredAction = requiredParts[0];

            for (String userPermission : userPermissions) {
                String[] userParts = userPermission.split(":");
                String userAction = userParts[0];
                String userResource = userParts[1];

                if (requiredPermission.equals(userPermission)) {
                    pass = true;
                    break;
                }

                if (userAction.equals(requiredAction) && userResource.equals("*")) {
                    pass = true;
                    break;
                }
            }

            if (!pass) {
                throw new HttpException(Constant.ErrorCode.NOT_ALLOWED_OPERATION, HttpStatus.FORBIDDEN);
            }
        }

        try {
            return pjp.proceed();
        } catch (Throwable throwable) {
            throw new HttpException(throwable.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
