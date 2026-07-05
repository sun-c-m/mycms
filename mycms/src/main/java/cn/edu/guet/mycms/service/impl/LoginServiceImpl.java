package cn.edu.guet.mycms.service.impl;

import cn.edu.guet.mycms.dto.LoginDTO;
import cn.edu.guet.mycms.entity.MenuType;
import cn.edu.guet.mycms.entity.Permission;
import cn.edu.guet.mycms.mapper.LoginMapper;
import cn.edu.guet.mycms.service.LoginService;
import cn.edu.guet.mycms.vo.UserLoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LoginServiceImpl implements LoginService {
    @Autowired
    private LoginMapper loginMapper;
    @Override
    public UserLoginVO login(LoginDTO loginDTO) {
        UserLoginVO loginVO = loginMapper.login(loginDTO);
        if (loginVO == null) {
            return null;
        }
        if(loginVO.getPermissionList()!=null){
            List<Permission> menuTree = buildMenuTree(loginVO.getPermissionList());
            loginVO.setMenuTree(menuTree);
        }
        return loginVO;
    }
    private List<Permission> buildMenuTree(List<Permission> permissions) {
        // 1. 过滤按钮
        List<Permission> menus = permissions.stream()
                .filter(p -> p.getMenuType() != null && !MenuType.BUTTON.equals(p.getMenuType()))
                .collect(Collectors.toList());

        // 2. Map分组（O(n) 高效挂载）
        Map<Integer, List<Permission>> childMap = menus.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() > 0)
                .collect(Collectors.groupingBy(Permission::getParentId));

        menus.forEach(p -> {
            List<Permission> children = childMap.get(p.getId());
            if (children != null) {
                // 3. 子节点排序（融合新方法的优点）
                children.sort(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)));
                p.setChildren(children);
            } else {
                p.setChildren(new ArrayList<>());
            }
        });

        // 4. 返回顶级节点并排序（融合新方法的优点）
        return menus.stream()
                .filter(p -> p.getParentId() != null && p.getParentId() == 0)
                .sorted(Comparator.comparing(Permission::getSort, Comparator.nullsLast(Integer::compareTo)))
                .collect(Collectors.toList());
    }

}
