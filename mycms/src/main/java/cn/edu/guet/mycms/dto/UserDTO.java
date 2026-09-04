package cn.edu.guet.mycms.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserDTO {
    private String username;
    private String password;
    private List<Integer> roleIds = new ArrayList<>();
}

