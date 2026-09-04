package cn.edu.guet.mycms.service;

import cn.edu.guet.mycms.dto.RoleAuthorizeDTO;
import cn.edu.guet.mycms.dto.RoleDTO;
import cn.edu.guet.mycms.entity.Permission;
import cn.edu.guet.mycms.entity.Role;
import cn.edu.guet.mycms.util.PageRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface RoleService {
    IPage<Role> getRoleInfoPage(PageRequest pageRequest);

    Role createRole(RoleDTO roleDTO);

    Role updateRole(Integer id, RoleDTO roleDTO);

    void deleteRole(Integer id);

    List<Permission> getPermissionTree();

    List<Integer> getRolePermissionIds(Integer roleId);

    void authorizeRole(RoleAuthorizeDTO roleAuthorizeDTO);
}

