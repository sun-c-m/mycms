package cn.edu.guet.mycms.aop;

import cn.edu.guet.mycms.annotation.RequiresPermission;
import cn.edu.guet.mycms.context.UserContext;
import cn.edu.guet.mycms.exception.PermissionException;
import cn.edu.guet.mycms.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class PermissionAspect {

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
                                  RequiresPermission requiresPermission) throws Throwable {
        UserLoginVO loginUser = UserContext.get();

        if (loginUser == null) {
            throw new RuntimeException("未登录");
        }

        String permission = requiresPermission.value();
        log.info("当前权限：{}",permission);
        if (!loginUser.hasPermission(permission)) {
            throw new PermissionException("无权限操作，请联系管理员");
        }

        return joinPoint.proceed();
    }
}

