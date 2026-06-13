package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;

import org.example.springboot.dto.*;
import org.example.springboot.entity.User;
import org.example.springboot.entity.UserLike;
import org.example.springboot.entity.Comment;
import org.example.springboot.entity.UserCollect;
import org.example.springboot.enums.AccountStatus;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.UserMapper;
import org.example.springboot.mapper.UserLikeMapper;
import org.example.springboot.mapper.CommentMapper;
import org.example.springboot.mapper.UserCollectMapper;
import org.example.springboot.service.convert.UserConvert;
import org.example.springboot.util.JwtTokenUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private UserLikeMapper userLikeMapper;
    @Resource
    private CommentMapper commentMapper;
    @Resource
    private UserCollectMapper userCollectMapper;
    
    @Resource
    private FileManagementService fileManagementService;

    @Value("${user.defaultPassword}")
    private String DEFAULT_PWD;

    @Resource
    private PasswordEncoder bCryptPasswordEncoder;

    /**
     * 根据邮箱获取用户信息(内部使用)
     */
    public User getByEmail(String email) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        return user;
    }

    public UserLoginResponseDTO login(UserLoginDTO loginDTO) {
        User user = UserConvert.toEntity(loginDTO);
        User dbUser = getByUsernameInternal(user.getUsername());
        // 用户存在性检查已经在 getByUsername 中处理
        if (dbUser.getStatus().equals(AccountStatus.PENDING_REVIEW.getValue())) {
            throw new ServiceException("账号正在审核");
        }
        if (dbUser.getStatus().equals(AccountStatus.REVIEW_FAILED.getValue())) {
            throw new ServiceException("账号审核不通过，请修改个人信息");
        }
        if (!bCryptPasswordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        
        String token = JwtTokenUtils.generateToken(dbUser.getId(), dbUser.getUsername(), dbUser.getRoleCode());
        
        // 设置token到临时字段
        dbUser.setToken(token);
        return UserConvert.toLoginResponseDTO(dbUser);
    }

    public List<UserResponseDTO> getUserByRole(String roleCode) {
        List<User> users = userMapper.selectList(
            new LambdaQueryWrapper<User>()
                .eq(User::getRoleCode, roleCode)
        );
        if (users.isEmpty()) {
            throw new ServiceException("未找到该角色的用户");
        }
        return UserConvert.toResponseDTOList(users);
    }

    public void createUser(UserCreateDTO createDTO) {
        // 检查用户名是否存在
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, createDTO.getUsername())
            ) != null) {
            throw new ServiceException("用户名已存在");
        }
        
        // 检查邮箱是否被使用
        if (userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, createDTO.getEmail())
            ) != null) {
            throw new ServiceException("邮箱已被使用");
        }

        User user = UserConvert.toEntity(createDTO);
        user.setPassword(StringUtils.isNotBlank(user.getPassword()) ? user.getPassword() : DEFAULT_PWD);
        user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        
        if (userMapper.insert(user) <= 0) {
            throw new ServiceException("用户创建失败");
        }
    }

    public void updateUser(Long id, UserUpdateDTO updateDTO) {
        // 检查用户是否存在
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new ServiceException("要更新的用户不存在");
        }
        
        // 检查新用户名是否与其他用户重复
        if (updateDTO.getUsername() != null) {
            User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, updateDTO.getUsername())
            );
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新用户名已被使用");
            }
        }
        
        // 检查新邮箱是否与其他用户重复
        if (updateDTO.getEmail() != null) {
            User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, updateDTO.getEmail())
            );
            if (existUser != null && !existUser.getId().equals(id)) {
                throw new ServiceException("新邮箱已被使用");
            }
        }
        
        // 处理头像更新 - 如果是新上传的临时文件，删除旧头像
        if (updateDTO.getAvatar() != null && updateDTO.getAvatar().contains("/temp/")) {
            try {
                // 删除旧头像
                if (existingUser.getAvatar() != null && !existingUser.getAvatar().isEmpty()) {
                    fileManagementService.deleteFileByUrl(existingUser.getAvatar());
                }
            } catch (Exception e) {
                // 删除失败不影响更新
            }
        }
        
        UserConvert.updateEntity(existingUser, updateDTO);
        if (userMapper.updateById(existingUser) <= 0) {
            throw new ServiceException("用户更新失败");
        }
    }

    /**
     * 根据用户名获取用户信息(对外接口)
     */
    public UserResponseDTO getByUsername(String username) {
        User user = getByUsernameInternal(username);
        return UserConvert.toResponseDTO(user);
    }
    
    /**
     * 根据用户名获取用户信息(内部使用)
     */
    private User getByUsernameInternal(String username) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public void deleteBatch(UserBatchDeleteDTO batchDeleteDTO) {
        List<Long> ids = batchDeleteDTO.getIds();
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择要删除的用户");
        }
        
        int result = userMapper.deleteByIds(ids);
        if (result <= 0) {
            throw new ServiceException("批量删除失败");
        }
    }

    public List<UserResponseDTO> getUserList() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<>());
        return UserConvert.toResponseDTOList(users);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = getUserByIdInternal(id);
        return UserConvert.toResponseDTO(user);
    }
    
    /**
     * 根据ID获取用户信息(内部使用)
     */
    public User getUserByIdInternal(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return user;
    }

    public Page<UserResponseDTO> getUsersByPage(UserQueryDTO queryDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(queryDTO.getUsername())) {
            queryWrapper.like(User::getUsername, queryDTO.getUsername());
        }

        if (StringUtils.isNotBlank(queryDTO.getName())) {
            queryWrapper.like(User::getName, queryDTO.getName());
        }
        if (StringUtils.isNotBlank(queryDTO.getRoleCode())) {
            queryWrapper.eq(User::getRoleCode, queryDTO.getRoleCode());
        }
        if (queryDTO.getStatus() != null) {
            queryWrapper.eq(User::getStatus, queryDTO.getStatus());
        }
        
        Page<User> userPage = userMapper.selectPage(new Page<>(queryDTO.getCurrentPage(), queryDTO.getSize()), queryWrapper);
        
        // 转换为DTO分页结果
        Page<UserResponseDTO> dtoPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        dtoPage.setRecords(UserConvert.toResponseDTOList(userPage.getRecords()));
        
        return dtoPage;
    }

    public void updatePassword(Long id, UserPasswordUpdateDTO update) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        
        // 验证旧密码
        if (!bCryptPasswordEncoder.matches(update.getOldPassword(), user.getPassword())) {
            throw new ServiceException("原密码错误");
        }
        
        // 更新新密码
        user.setPassword(bCryptPasswordEncoder.encode(update.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码修改失败");
        }
    }

    public void forgetPassword(UserForgetPasswordDTO forgetPasswordDTO) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getEmail, forgetPasswordDTO.getEmail())
        );
        if (user == null) {
            throw new ServiceException("邮箱不存在");
        }
        
        user.setPassword(bCryptPasswordEncoder.encode(forgetPasswordDTO.getNewPassword()));
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("密码重置失败");
        }
    }

    public void deleteUserById(Long id) {
        if (userMapper.deleteById(id) <= 0) {
            throw new ServiceException("删除失败");
        }
    }

    /**
     * 获取管理员用户信息
     * @return 管理员用户
     */
    public UserResponseDTO getAdminUser() {
        // 查询角色为ADMIN的第一个用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getRoleCode, "ADMIN");
        User adminUser = userMapper.selectOne(queryWrapper);
        
        if (adminUser == null) {
            throw new ServiceException("未找到管理员用户");
        }
        
        return UserConvert.toResponseDTO(adminUser);
    }
    
    /**
     * 更新用户状态
     */
    public void updateUserStatus(Long userId, UserStatusUpdateDTO statusUpdateDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        
        user.setStatus(statusUpdateDTO.getStatus());
        if (userMapper.updateById(user) <= 0) {
            throw new ServiceException("用户状态更新失败");
        }
    }
    
    /**
     * 获取用户统计数据
     */
    public UserStatsDTO getUserStats(Long userId) {
        UserStatsDTO stats = new UserStatsDTO();
        
        // 获取点赞数量
        LambdaQueryWrapper<UserLike> likeQuery = new LambdaQueryWrapper<>();
        likeQuery.eq(UserLike::getUserId, userId);
        stats.setLikeCount(userLikeMapper.selectCount(likeQuery));
        
        // 获取收藏数量
        LambdaQueryWrapper<UserCollect> collectQuery = new LambdaQueryWrapper<>();
        collectQuery.eq(UserCollect::getUserId, userId);
        stats.setCollectCount(userCollectMapper.selectCount(collectQuery));
        
        // 获取评论数量
        LambdaQueryWrapper<Comment> commentQuery = new LambdaQueryWrapper<>();
        commentQuery.eq(Comment::getUserId, userId);
        stats.setCommentCount(commentMapper.selectCount(commentQuery));
        
        return stats;
    }
    
}
