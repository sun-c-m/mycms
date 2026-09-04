package cn.edu.guet.mycms.service.impl;

import cn.edu.guet.mycms.dto.PermissionDTO;
import cn.edu.guet.mycms.entity.MenuType;
import cn.edu.guet.mycms.entity.Permission;
import cn.edu.guet.mycms.mapper.PermissionMapper;
import cn.edu.guet.mycms.service.PermissionService;
import cn.edu.guet.mycms.util.PageRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Autowired
    private PermissionMapper permissionMapper;

    @Override
    public IPage<Permission> getPermissionPage(PageRequest pageRequest) {
        Page<Permission> page = new Page<>(pageRequest.getCurrentPage(), pageRequest.getPageSize());
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();

        String name = pageRequest.getParamValue("name");
        String code = pageRequest.getParamValue("code");
        String menuType = pageRequest.getParamValue("menuType");
        if (StringUtils.hasText(name)) {
            queryWrapper.like("name", name);
        }
        if (StringUtils.hasText(code)) {
            queryWrapper.like("code", code);
        }
        if (StringUtils.hasText(menuType)) {
            queryWrapper.eq("type", menuType);
        }
        queryWrapper.orderByAsc("parent_id", "sort", "id");

        return permissionMapper.selectPage(page, queryWrapper);
    }

    @Override
    public List<Permission> getPermissionTree() {
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("parent_id", "sort", "id");
        return buildPermissionTree(permissionMapper.selectList(queryWrapper));
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Permission createPermission(PermissionDTO permissionDTO) {
        validatePermission(permissionDTO, null);
        ensureCodeAvailable(permissionDTO.getCode(), null);

        Permission permission = new Permission();
        fillPermission(permission, permissionDTO);
        permissionMapper.insert(permission);
        return permissionMapper.selectById(permission.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Permission updatePermission(Integer id, PermissionDTO permissionDTO) {
        if (id == null) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        validatePermission(permissionDTO, id);
        ensureCodeAvailable(permissionDTO.getCode(), id);
        fillPermission(permission, permissionDTO);
        permissionMapper.updateById(permission);
        return permissionMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deletePermission(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("权限ID不能为空");
        }
        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        QueryWrapper<Permission> childWrapper = new QueryWrapper<>();
        childWrapper.eq("parent_id", id);
        if (permissionMapper.selectCount(childWrapper) > 0) {
            throw new IllegalArgumentException("当前权限存在子权限，请先删除子权限");
        }

        permissionMapper.deleteById(id);
    }

    private void validatePermission(PermissionDTO permissionDTO, Integer currentPermissionId) {
        if (permissionDTO == null || !StringUtils.hasText(permissionDTO.getName())) {
            throw new IllegalArgumentException("权限名称不能为空");
        }
        if (permissionDTO.getMenuType() == null) {
            throw new IllegalArgumentException("权限类型不能为空");
        }

        Integer parentId = permissionDTO.getParentId() == null ? 0 : permissionDTO.getParentId();
        if (parentId < 0) {
            throw new IllegalArgumentException("父级权限不正确");
        }
        if (currentPermissionId != null && parentId.equals(currentPermissionId)) {
            throw new IllegalArgumentException("父级权限不能选择自己");
        }
        if (parentId > 0) {
            Permission parent = permissionMapper.selectById(parentId);
            if (parent == null) {
                throw new IllegalArgumentException("父级权限不存在");
            }
            if (currentPermissionId != null && isDescendant(parentId, currentPermissionId)) {
                throw new IllegalArgumentException("父级权限不能选择自己的子权限");
            }
        }
    }

    private void ensureCodeAvailable(String code, Integer currentPermissionId) {
        if (!StringUtils.hasText(code)) {
            return;
        }
        QueryWrapper<Permission> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("code", code);
        Permission permission = permissionMapper.selectOne(queryWrapper);
        if (permission != null && (currentPermissionId == null || !permission.getId().equals(currentPermissionId))) {
            throw new IllegalArgumentException("权限标识已存在");
        }
    }

    private boolean isDescendant(Integer possibleDescendantId, Integer ancestorId) {
        Permission current = permissionMapper.selectById(possibleDescendantId);
        while (current != null && current.getParentId() != null && current.getParentId() > 0) {
            if (Objects.equals(current.getParentId(), ancestorId)) {
                return true;
            }
            current = permissionMapper.selectById(current.getParentId());
        }
        return false;
    }

    private void fillPermission(Permission permission, PermissionDTO permissionDTO) {
        BeanUtils.copyProperties(permissionDTO, permission);
        permission.setParentId(permissionDTO.getParentId() == null ? 0 : permissionDTO.getParentId());
        permission.setSort(permissionDTO.getSort() == null ? 0 : permissionDTO.getSort());
        if (permission.getMenuType() == MenuType.BUTTON && !StringUtils.hasText(permission.getPath())) {
            permission.setPath(null);
        }
        if (!StringUtils.hasText(permission.getCode())) {
            permission.setCode(null);
        }
        if (!StringUtils.hasText(permission.getPath())) {
            permission.setPath(null);
        }
    }

    private List<Permission> buildPermissionTree(List<Permission> permissions) {
        List<Permission> tree = new ArrayList<>();
        for (Permission permission : permissions) {
            if (permission.getParentId() == null || permission.getParentId() == 0) {
                tree.add(findChildren(permission, permissions));
            }
        }
        tree.sort(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)));
        return tree;
    }

    private Permission findChildren(Permission parent, List<Permission> permissions) {
        parent.setChildren(permissions.stream()
                .filter(permission -> parent.getId().equals(permission.getParentId()))
                .map(permission -> findChildren(permission, permissions))
                .sorted(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList()));
        return parent;
    }
}

