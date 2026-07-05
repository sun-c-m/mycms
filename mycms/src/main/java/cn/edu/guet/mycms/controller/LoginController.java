package cn.edu.guet.mycms.controller;

import cn.edu.guet.mycms.dto.LoginDTO;
import cn.edu.guet.mycms.service.LoginService;
import cn.edu.guet.mycms.vo.UserLoginVO;
import cn.edu.guet.mycms.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/login")
public class LoginController {
    @Autowired
    private LoginService loginService;

    @PostMapping("/getUser")
    public Result<Object> login(@RequestBody LoginDTO loginDTO) {
        log.info("用户名：{}，密码： {}", loginDTO.getUsername(), loginDTO.getPassword());
        UserLoginVO loginVO = loginService.login(loginDTO);
        if (loginVO != null) {
            return Result.success("登录成功",loginVO);
        }
        return Result.fail(401, "登录失败");
    }
}
