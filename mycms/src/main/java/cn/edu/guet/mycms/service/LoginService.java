package cn.edu.guet.mycms.service;

import cn.edu.guet.mycms.dto.LoginDTO;
import cn.edu.guet.mycms.vo.UserLoginVO;

public interface LoginService {
    UserLoginVO login(LoginDTO loginDTO);
}
