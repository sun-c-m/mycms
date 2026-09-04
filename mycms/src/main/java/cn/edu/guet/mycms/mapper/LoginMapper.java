package cn.edu.guet.mycms.mapper;

import cn.edu.guet.mycms.dto.LoginDTO;
import cn.edu.guet.mycms.vo.UserLoginVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginMapper {
    UserLoginVO login(LoginDTO loginDTO);
}

