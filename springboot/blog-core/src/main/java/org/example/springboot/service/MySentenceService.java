package org.example.springboot.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.dto.MySentenceCreateDTO;
import org.example.springboot.dto.MySentenceResponseDTO;
import org.example.springboot.dto.MySentenceUpdateDTO;
import org.example.springboot.entity.MySentence;
import org.example.springboot.exception.BusinessException;
import org.example.springboot.mapper.MySentenceMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * 我的句子 Service
 */
@Slf4j
@Service
public class MySentenceService {

    @Resource
    private MySentenceMapper mySentenceMapper;

    /**
     * 分页查询句子列表
     *
     * @param current    当前页码
     * @param size       每页大小
     * @param keyword    关键词搜索
     * @param isHomepage 是否首页展示
     * @return 分页结果
     */
    public Page<MySentenceResponseDTO> getPage(Long current, Long size, String keyword, Integer isHomepage) {
        log.info("分页查询句子列表: current={}, size={}, keyword={}, isHomepage={}", current, size, keyword, isHomepage);

        Page<MySentence> page = new Page<>(current, size);
        LambdaQueryWrapper<MySentence> wrapper = new LambdaQueryWrapper<>();

        // 关键词搜索（搜索句子内容和关键词字段）
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(MySentence::getSentenceContent, keyword)
                    .or()
                    .like(MySentence::getKeywords, keyword));
        }

        // 是否首页展示筛选
        if (isHomepage != null) {
            wrapper.eq(MySentence::getIsHomepage, isHomepage);
        }

        // 按创建时间倒序
        wrapper.orderByDesc(MySentence::getCreatedAt);

        Page<MySentence> resultPage = mySentenceMapper.selectPage(page, wrapper);

        // 转换为 DTO
        Page<MySentenceResponseDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<MySentenceResponseDTO> dtoList = resultPage.getRecords().stream()
                .map(this::convertToDTO)
                .toList();
        dtoPage.setRecords(dtoList);

        return dtoPage;
    }

    /**
     * 根据ID获取句子详情
     *
     * @param id 句子ID
     * @return 句子详情
     */
    public MySentenceResponseDTO getById(Integer id) {
        log.info("获取句子详情: id={}", id);
        MySentence sentence = mySentenceMapper.selectById(id);
        if (sentence == null) {
            throw new BusinessException("句子不存在");
        }
        return convertToDTO(sentence);
    }

    /**
     * 创建句子
     *
     * @param createDTO 创建参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(MySentenceCreateDTO createDTO) {
        log.info("创建句子: {}", createDTO);

        // 验证关键词必须包含在句子内容中
        validateKeywords(createDTO.getSentenceContent(), createDTO.getKeywords());

        MySentence sentence = new MySentence();
        BeanUtil.copyProperties(createDTO, sentence);
        mySentenceMapper.insert(sentence);

        log.info("句子创建成功: id={}", sentence.getId());
    }

    /**
     * 更新句子
     *
     * @param id        句子ID
     * @param updateDTO 更新参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Integer id, MySentenceUpdateDTO updateDTO) {
        log.info("更新句子: id={}, dto={}", id, updateDTO);

        MySentence existing = mySentenceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("句子不存在");
        }

        // 验证关键词必须包含在句子内容中
        validateKeywords(updateDTO.getSentenceContent(), updateDTO.getKeywords());

        BeanUtil.copyProperties(updateDTO, existing, "id", "createdAt");
        mySentenceMapper.updateById(existing);

        log.info("句子更新成功: id={}", id);
    }

    /**
     * 删除句子
     *
     * @param id 句子ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        log.info("删除句子: id={}", id);

        MySentence existing = mySentenceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("句子不存在");
        }

        mySentenceMapper.deleteById(id);
        log.info("句子删除成功: id={}", id);
    }

    /**
     * 切换首页展示状态
     *
     * @param id         句子ID
     * @param isHomepage 是否首页展示
     */
    @Transactional(rollbackFor = Exception.class)
    public void toggleHomepage(Integer id, Integer isHomepage) {
        log.info("切换首页展示状态: id={}, isHomepage={}", id, isHomepage);

        MySentence existing = mySentenceMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("句子不存在");
        }

        existing.setIsHomepage(isHomepage);
        mySentenceMapper.updateById(existing);

        log.info("首页展示状态切换成功: id={}, isHomepage={}", id, isHomepage);
    }

    /**
     * 获取随机首页句子
     *
     * @return 随机句子（如果没有则返回null）
     */
    public MySentenceResponseDTO getRandomHomepageSentence() {
        log.info("获取随机首页句子");

        LambdaQueryWrapper<MySentence> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MySentence::getIsHomepage, 1);

        List<MySentence> sentences = mySentenceMapper.selectList(wrapper);

        if (sentences.isEmpty()) {
            log.info("没有设置首页展示的句子");
            return null;
        }

        // 随机选择一条
        Random random = new Random();
        MySentence randomSentence = sentences.get(random.nextInt(sentences.size()));

        log.info("随机获取到句子: id={}", randomSentence.getId());
        return convertToDTO(randomSentence);
    }

    /**
     * 验证关键词必须包含在句子内容中
     *
     * @param content  句子内容
     * @param keywords 关键词
     */
    private void validateKeywords(String content, String keywords) {
        if (StrUtil.isBlank(keywords)) {
            return;
        }

        String[] keywordArray = keywords.split(",");
        for (String keyword : keywordArray) {
            String trimmedKeyword = keyword.trim();
            if (StrUtil.isNotBlank(trimmedKeyword) && !content.contains(trimmedKeyword)) {
                throw new BusinessException("关键词 \"" + trimmedKeyword + "\" 必须包含在句子内容中");
            }
        }
    }

    /**
     * Entity 转 DTO
     */
    private MySentenceResponseDTO convertToDTO(MySentence entity) {
        MySentenceResponseDTO dto = new MySentenceResponseDTO();
        BeanUtil.copyProperties(entity, dto);
        return dto;
    }
}
