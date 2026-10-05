package com.example.gamerecord.service;

import com.example.gamerecord.common.BizException;
import com.example.gamerecord.common.ResultCode;
import com.example.gamerecord.entity.SensitiveWord;
import com.example.gamerecord.entity.User;
import com.example.gamerecord.mapper.SensitiveWordMapper;
import com.example.gamerecord.mapper.UserMapper;
import com.example.gamerecord.util.UserContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 敏感词服务
 *
 * <p>词库存数据库，启动时加载到内存缓存；管理员增删后自动刷新缓存，
 * 评论过滤直接读缓存，避免每次评论都查库。</p>
 */
@Service
@RequiredArgsConstructor
public class SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final UserMapper userMapper;

    /** 内存缓存（volatile 保证增删后对其他线程可见） */
    private volatile Set<String> cache = new HashSet<>();

    /** 启动时加载词库 */
    @PostConstruct
    public void reload() {
        List<SensitiveWord> all = sensitiveWordMapper.selectList(null);
        cache = all.stream().map(SensitiveWord::getWord).collect(Collectors.toCollection(HashSet::new));
    }

    /** 当前生效的敏感词集合（评论过滤用） */
    public Set<String> getWords() {
        return cache;
    }

    /** 敏感词列表（仅管理员；返回 id+word 供前端删除定位） */
    public List<Map<String, Object>> list() {
        requireAdmin();
        return sensitiveWordMapper.selectList(null).stream()
                .map(sw -> Map.<String, Object>of("id", sw.getId(), "word", sw.getWord()))
                .toList();
    }

    /** 添加敏感词（仅管理员；重复词忽略） */
    public void add(String word) {
        requireAdmin();
        if (!StringUtils.hasText(word)) {
            throw new BizException(ResultCode.BAD_REQUEST, "敏感词不能为空");
        }
        String trimmed = word.trim();
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(trimmed);
        sensitiveWordMapper.insert(sw);
        reload();
    }

    /** 删除敏感词（仅管理员） */
    public void delete(Long id) {
        requireAdmin();
        sensitiveWordMapper.deleteById(id);
        reload();
    }

    /** 管理员校验 */
    private void requireAdmin() {
        Long userId = UserContext.require();
        User user = userMapper.selectById(userId);
        if (user == null || !"admin".equals(user.getRole())) {
            throw new BizException(ResultCode.FORBIDDEN, "仅管理员可操作");
        }
    }
}
