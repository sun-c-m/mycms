package cn.edu.guet.mycms.dto;

import cn.edu.guet.mycms.entity.MenuType;
import lombok.Data;

@Data
public class PermissionDTO {
    private String name;
    private MenuType menuType;
    private String code;
    private String path;
    private Integer parentId;
    private Integer sort;
}

