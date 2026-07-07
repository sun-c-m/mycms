package cn.edu.guet.mycms.mapper;

import cn.edu.guet.mycms.dto.LoginDTO;
import cn.edu.guet.mycms.vo.UserLoginVO;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
@Mapper
public interface LoginMapper {
    UserLoginVO login(LoginDTO loginDTO);
}
