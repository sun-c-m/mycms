package cn.edu.guet.mycms.service;

import cn.edu.guet.mycms.dto.PermissionDTO;
import cn.edu.guet.mycms.entity.Permission;
import cn.edu.guet.mycms.util.PageRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface PermissionService {
    IPage<Permission> getPermissionPage(PageRequest pageRequest);

    List<Permission> getPermissionTree();

    Permission createPermission(PermissionDTO permissionDTO);

    Permission updatePermission(Integer id, PermissionDTO permissionDTO);

    void deletePermission(Integer id);
}

