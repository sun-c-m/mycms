package cn.edu.guet.cms3.service.impl;

import cn.edu.guet.cms3.dto.NewsAttachmentDTO;
import cn.edu.guet.cms3.dto.NewsCreateDTO;
import cn.edu.guet.cms3.entity.News;
import cn.edu.guet.cms3.entity.NewsAttachment;
import cn.edu.guet.cms3.mapper.NewsAttachmentMapper;
import cn.edu.guet.cms3.mapper.NewsMapper;
import cn.edu.guet.cms3.service.NewsService;
import cn.edu.guet.cms3.util.PageRequest;
import cn.edu.guet.cms3.vo.NewsVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NewsServiceImpl implements NewsService {

    private static final String PENDING_REVIEW = "PENDING_REVIEW";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String REJECTED = "REJECTED";

    @Value("${news.upload-dir:/Users/liwei/Desktop/upload}")
    private String uploadDir;

    @Autowired
    private NewsMapper newsMapper;

    @Autowired
    private NewsAttachmentMapper newsAttachmentMapper;


    @Override
    public IPage<NewsVO> getNewsPage(PageRequest pageRequest) {
        Page<News> page = new Page<>(pageRequest.getCurrentPage(), pageRequest.getPageSize());
        QueryWrapper<News> queryWrapper = new QueryWrapper<>();

        String keyword = pageRequest.getParamValue("keyword");
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("supplier", keyword)
                    .or()
                    .like("reviewer", keyword));
        }

        String category = pageRequest.getParamValue("category");
        if (StringUtils.hasText(category)) {
            queryWrapper.eq("category", category);
        }

        String status = pageRequest.getParamValue("status");
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }

        queryWrapper.orderByDesc("update_time", "id");

        return newsMapper.selectPage(page, queryWrapper).convert(this::toVO);
    }

    @Override
    public IPage<NewsVO> getPublicNewsPage(PageRequest pageRequest) {
        PageRequest safePageRequest = pageRequest == null ? new PageRequest() : pageRequest;
        Page<News> page = new Page<>(safePageRequest.getCurrentPage(), safePageRequest.getPageSize());
        QueryWrapper<News> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("status", PUBLISHED);

        String keyword = safePageRequest.getParamValue("keyword");
        if (StringUtils.hasText(keyword)) {
            queryWrapper.and(wrapper -> wrapper
                    .like("title", keyword)
                    .or()
                    .like("supplier", keyword)
                    .or()
                    .like("reviewer", keyword));
        }

        String category = safePageRequest.getParamValue("category");
        if (StringUtils.hasText(category)) {
            queryWrapper.eq("category", category);
        }

        queryWrapper.orderByDesc("publish_time", "update_time", "id");

        return newsMapper.selectPage(page, queryWrapper).convert(this::toVO);
    }

    @Override
    public NewsVO getNewsDetail(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }


        return toVO(news);
    }

    @Override
    public NewsVO getPublicNewsDetail(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null || !PUBLISHED.equals(news.getStatus())) {
            throw new IllegalArgumentException("新闻不存在或尚未发布");
        }

        return toVO(news);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO createNews(NewsCreateDTO newsCreateDTO) {
        validateNews(newsCreateDTO);

        News news = new News();
        news.setTitle(newsCreateDTO.getTitle());
        news.setCategory(newsCreateDTO.getCategory());
        news.setSupplier(newsCreateDTO.getSupplier());
        news.setReviewer(newsCreateDTO.getReviewer());
        news.setContent(newsCreateDTO.getContent());
        news.setStatus(PENDING_REVIEW);

        newsMapper.insert(news);

        return getNewsDetail(news.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO createNewsWithFiles(NewsCreateDTO newsCreateDTO, List<MultipartFile> files) throws IOException {
        NewsVO newsVO = createNews(newsCreateDTO);
        insertUploadedAttachments(newsVO.getId(), files);
        return getNewsDetail(newsVO.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO updateNews(Long id, NewsCreateDTO newsCreateDTO) {
        validateNews(newsCreateDTO);

        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }

        news.setTitle(newsCreateDTO.getTitle());
        news.setCategory(newsCreateDTO.getCategory());
        news.setSupplier(newsCreateDTO.getSupplier());
        news.setReviewer(newsCreateDTO.getReviewer());
        news.setContent(newsCreateDTO.getContent());
        news.setStatus(PENDING_REVIEW);
        news.setPublishTime(null);

        newsMapper.updateById(news);

        rebuildAttachments(id, newsCreateDTO.getAttachments());

        return getNewsDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO updateNewsWithFiles(Long id, NewsCreateDTO newsCreateDTO, List<MultipartFile> files) throws IOException {
        NewsVO newsVO = updateNews(id, newsCreateDTO);
        insertUploadedAttachments(id, files);
        return getNewsDetail(newsVO.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        QueryWrapper<NewsAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("news_id", id);
        List<NewsAttachment> attachments = newsAttachmentMapper.selectList(queryWrapper);
        newsAttachmentMapper.delete(queryWrapper);
        newsMapper.deleteById(id);
        attachments.forEach(this::deleteAttachmentFile);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO approveNews(Long id) {
        News news = getReviewableNews(id);
        news.setStatus(PUBLISHED);
        news.setPublishTime(LocalDateTime.now());
        newsMapper.updateById(news);
        return getNewsDetail(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public NewsVO rejectNews(Long id) {
        News news = getReviewableNews(id);
        news.setStatus(REJECTED);
        news.setPublishTime(null);
        newsMapper.updateById(news);
        return getNewsDetail(id);
    }

    private News getReviewableNews(Long id) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new IllegalArgumentException("新闻不存在");
        }
        if (!PENDING_REVIEW.equals(news.getStatus())) {
            throw new IllegalArgumentException("只有待审核新闻可以审核");
        }
        return news;
    }

    private void validateNews(NewsCreateDTO newsCreateDTO) {
        if (newsCreateDTO == null || !StringUtils.hasText(newsCreateDTO.getTitle())) {
            throw new IllegalArgumentException("新闻标题不能为空");
        }
        if (!StringUtils.hasText(newsCreateDTO.getCategory())) {
            throw new IllegalArgumentException("栏目不能为空");
        }
        if (!StringUtils.hasText(newsCreateDTO.getContent())) {
            throw new IllegalArgumentException("正文内容不能为空");
        }
    }

    private NewsVO toVO(News news) {
        NewsVO newsVO = new NewsVO();
        BeanUtils.copyProperties(news, newsVO);
        QueryWrapper<NewsAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("news_id", news.getId()).orderByAsc("id");
        List<NewsAttachmentDTO> attachments = newsAttachmentMapper.selectList(queryWrapper)
                .stream()
                .map(this::toAttachmentDTO)
                .toList();
        newsVO.setAttachments(attachments);
        return newsVO;
    }

    private void insertUploadedAttachments(Long newsId, List<MultipartFile> files) throws IOException {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }

            NewsAttachmentDTO attachmentDTO = uploadAttachment(file);
            NewsAttachment attachment = new NewsAttachment();
            attachment.setNewsId(newsId);
            attachment.setName(attachmentDTO.getName());
            attachment.setUrl(attachmentDTO.getUrl());
            attachment.setFileSize(attachmentDTO.getSize());
            attachment.setFileType(attachmentDTO.getType());
            newsAttachmentMapper.insert(attachment);
        }
    }

    private void rebuildAttachments(Long newsId, List<NewsAttachmentDTO> keptAttachments) {
        QueryWrapper<NewsAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("news_id", newsId);
        List<NewsAttachment> existingAttachments = newsAttachmentMapper.selectList(queryWrapper);

        List<NewsAttachmentDTO> safeKeptAttachments = keptAttachments == null ? Collections.emptyList() : keptAttachments;
        Set<String> keptUrls = safeKeptAttachments.stream()
                .filter(attachment -> attachment != null && StringUtils.hasText(attachment.getUrl()))
                .map(NewsAttachmentDTO::getUrl)
                .collect(Collectors.toSet());

        newsAttachmentMapper.delete(queryWrapper);

        for (NewsAttachmentDTO attachmentDTO : safeKeptAttachments) {
            if (attachmentDTO == null || !StringUtils.hasText(attachmentDTO.getName()) || !StringUtils.hasText(attachmentDTO.getUrl())) {
                continue;
            }
            NewsAttachment attachment = new NewsAttachment();
            attachment.setNewsId(newsId);
            attachment.setName(attachmentDTO.getName());
            attachment.setUrl(attachmentDTO.getUrl());
            attachment.setFileSize(attachmentDTO.getSize());
            attachment.setFileType(attachmentDTO.getType());
            newsAttachmentMapper.insert(attachment);
        }

        existingAttachments.stream()
                .filter(attachment -> !keptUrls.contains(attachment.getUrl()))
                .forEach(this::deleteAttachmentFile);
    }

    private NewsAttachmentDTO uploadAttachment(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String originalFilename = Objects.requireNonNullElse(file.getOriginalFilename(), "attachment");
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + (StringUtils.hasText(extension) ? "." + extension : "");
        Path targetPath = uploadPath.resolve(storedFilename).normalize();

        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        NewsAttachmentDTO attachmentDTO = new NewsAttachmentDTO();
        attachmentDTO.setName(originalFilename);
        attachmentDTO.setUrl("/api/news/attachments/" + storedFilename);
        attachmentDTO.setSize(file.getSize());
        attachmentDTO.setType(file.getContentType());
        return attachmentDTO;
    }

    private NewsAttachmentDTO toAttachmentDTO(NewsAttachment attachment) {
        NewsAttachmentDTO attachmentDTO = new NewsAttachmentDTO();
        attachmentDTO.setName(attachment.getName());
        attachmentDTO.setUrl(attachment.getUrl());
        attachmentDTO.setSize(attachment.getFileSize());
        attachmentDTO.setType(attachment.getFileType());
        return attachmentDTO;
    }

    private void deleteAttachmentFile(NewsAttachment attachment) {
        if (attachment == null || !StringUtils.hasText(attachment.getUrl())) {
            return;
        }

        String filename = attachment.getUrl().substring(attachment.getUrl().lastIndexOf('/') + 1);
        if (!StringUtils.hasText(filename)) {
            return;
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path targetPath = uploadPath.resolve(filename).normalize();
            if (!targetPath.startsWith(uploadPath)) {
                return;
            }
            Files.deleteIfExists(targetPath);
        } catch (IOException exception) {
            log.warn("删除新闻附件文件失败，url={}", attachment.getUrl(), exception);
        }
    }
}
