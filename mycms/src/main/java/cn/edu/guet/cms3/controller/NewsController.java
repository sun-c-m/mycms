package cn.edu.guet.cms3.controller;

import cn.edu.guet.cms3.annotation.RequiresPermission;
import cn.edu.guet.cms3.dto.NewsCreateDTO;
import cn.edu.guet.cms3.entity.NewsAttachment;
import cn.edu.guet.cms3.mapper.NewsAttachmentMapper;
import cn.edu.guet.cms3.service.NewsService;
import cn.edu.guet.cms3.util.PageRequest;
import cn.edu.guet.cms3.util.Result;
import cn.edu.guet.cms3.vo.NewsVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Value("${news.upload-dir:/Users/liwei/Desktop/upload}")
    private String uploadDir;

    @Autowired
    private NewsService newsService;

    @Autowired
    private NewsAttachmentMapper newsAttachmentMapper;


    @PostMapping("/page")
    public Result<IPage<NewsVO>> getNewsPage(@RequestBody PageRequest pageRequest) {
        return Result.success(newsService.getNewsPage(pageRequest));
    }

    @PostMapping("/public/page")
    public Result<IPage<NewsVO>> getPublicNewsPage(@RequestBody(required = false) PageRequest pageRequest) {
        return Result.success(newsService.getPublicNewsPage(pageRequest));
    }

    @GetMapping("/public/{id}")
    public Result<NewsVO> getPublicNewsDetail(@PathVariable Long id) {
        return Result.success(newsService.getPublicNewsDetail(id));
    }

    @PostMapping
    public Result<NewsVO> createNews(@RequestBody NewsCreateDTO newsCreateDTO) {
        return Result.success("新闻已提交审核", newsService.createNews(newsCreateDTO));
    }

    @PostMapping(value = "/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NewsVO> createNewsWithFiles(
            @RequestPart("news") NewsCreateDTO newsCreateDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        return Result.success("新闻已提交审核", newsService.createNewsWithFiles(newsCreateDTO, files));
    }

    @GetMapping("/attachments/{filename}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String filename) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path filePath = uploadPath.resolve(filename).normalize();
        if (!filePath.startsWith(uploadPath)) {
            return ResponseEntity.badRequest().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(filePath);
        MediaType mediaType = StringUtils.hasText(contentType)
                ? MediaType.parseMediaType(contentType)
                : MediaType.APPLICATION_OCTET_STREAM;
        boolean previewable = mediaType.getType().equals("image") || MediaType.APPLICATION_PDF.equals(mediaType);
        String originalFilename = getOriginalAttachmentName(filename, resource.getFilename());
        ContentDisposition contentDisposition = ContentDisposition
                .builder(previewable ? "inline" : "attachment")
                .filename(originalFilename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    @PutMapping("/{id}")
    public Result<NewsVO> updateNews(
            @PathVariable Long id,
            @RequestBody NewsCreateDTO newsCreateDTO
    ) {
        return Result.success("新闻已更新并提交审核", newsService.updateNews(id, newsCreateDTO));
    }

    @PostMapping(value = "/{id}/form", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<NewsVO> updateNewsWithFiles(
            @PathVariable Long id,
            @RequestPart("news") NewsCreateDTO newsCreateDTO,
            @RequestPart(value = "files", required = false) List<MultipartFile> files
    ) throws IOException {
        return Result.success("新闻已更新并提交审核", newsService.updateNewsWithFiles(id, newsCreateDTO, files));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteNews(@PathVariable Long id) {
        newsService.deleteNews(id);
        return Result.success("新闻已删除");
    }

    @RequiresPermission("content:news:audit")
    @PutMapping("/{id}/approve")
    public Result<NewsVO> approveNews(@PathVariable Long id) {
        return Result.success("新闻审核已通过", newsService.approveNews(id));
    }

    @RequiresPermission("content:news:audit")
    @PutMapping("/{id}/reject")
    public Result<NewsVO> rejectNews(@PathVariable Long id) {
        return Result.success("新闻已驳回", newsService.rejectNews(id));
    }

    private String getOriginalAttachmentName(String filename, String fallbackName) {
        QueryWrapper<NewsAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("url", "/api/news/attachments/" + filename).last("limit 1");
        NewsAttachment attachment = newsAttachmentMapper.selectOne(queryWrapper);
        return attachment == null || attachment.getName() == null ? fallbackName : attachment.getName();
    }
}
