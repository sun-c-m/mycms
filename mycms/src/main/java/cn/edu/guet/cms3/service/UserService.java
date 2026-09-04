package cn.edu.guet.cms3.service;

import cn.edu.guet.cms3.dto.UserDTO;
import cn.edu.guet.cms3.util.PageRequest;
import cn.edu.guet.cms3.vo.UserVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.transaction.annotation.Transactional;

public interface UserService {

    IPage<UserVO> getUserPage(PageRequest pageRequest);

    @Transactional(rollbackFor = Exception.class)
    UserVO createUser(UserDTO userDTO);

    @Transactional(rollbackFor = Exception.class)
    UserVO updateUser(Long id, UserDTO userDTO);

    @Transactional(rollbackFor = Exception.class)
    void deleteUser(Long id);
}
