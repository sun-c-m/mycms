package cn.edu.guet.cms3.vo;

import cn.edu.guet.cms3.dto.NewsAttachmentDTO;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Data
public class NewsVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String title;
    private String category;
    private String supplier;
    private String reviewer;
    private String content;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime publishTime;
    private List<NewsAttachmentDTO> attachments = new ArrayList<>();
}
