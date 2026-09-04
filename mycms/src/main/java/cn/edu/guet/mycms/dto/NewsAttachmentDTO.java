package cn.edu.guet.mycms.dto;

import lombok.Data;

@Data
public class NewsAttachmentDTO {
    private String name;
    private String url;
    private Long size;
    private String type;
}

