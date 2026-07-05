package cn.edu.guet.mycms.vo;

import cn.edu.guet.mycms.entity.Permission;
import lombok.Data;

import java.util.List;

@Data
public class UserLoginVO {
    private String username;
    private Integer id;
    private List<Permission> menuTree;
    private List<Permission> permissionList;
}
