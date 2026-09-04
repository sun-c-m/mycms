package cn.edu.guet.mycms.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoleAuthorizeDTO {
    private Integer roleId;
    private List<Integer> permissionIds = new ArrayList<>();
}

