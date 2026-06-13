package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.dto.*;
import org.example.springboot.service.UserService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Tag(name="用户管理接口")
@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    @Resource
    private UserService userService;

    @Operation(summary = "根据id获取用户信息")
    @GetMapping("/{id}")
    public Result<UserResponseDTO> getById(@PathVariable Long id) {
        // 如果用户不存在会抛出异常，由全局异常处理器处理
        UserResponseDTO user = userService.getUserById(id);
        return Result.success(user);
    }

    @Operation(summary = "根据username获取用户信息")
    @GetMapping("/username/{username}")
    public Result<UserResponseDTO> getUserByUsername(@PathVariable String username) {
        // 不存在的用户会抛出异常
        UserResponseDTO user = userService.getByUsername(username);
        return Result.success(user);
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@RequestBody UserLoginDTO loginDTO) {
        UserLoginResponseDTO loginUser = userService.login(loginDTO);
        return Result.success(loginUser);
    }

    @Operation(summary = "密码修改")
    @PutMapping("/password/{id}")
    public Result<?> updatePassword(@PathVariable Long id, @RequestBody UserPasswordUpdateDTO userPasswordUpdate) {
        // 密码修改失败会抛出异常
        userService.updatePassword(id, userPasswordUpdate);
        return Result.success();
    }

    @Operation(summary = "忘记密码")
    @PostMapping("/forget")
    public Result<?> forgetPassword(@RequestBody UserForgetPasswordDTO forgetPasswordDTO) {
        // 密码重置失败会抛出异常
        userService.forgetPassword(forgetPasswordDTO);
        return Result.success();
    }

    @Operation(summary = "分页查询用户")
    @GetMapping("/page")
    public Result<Page<UserResponseDTO>> getUsersByPage(UserQueryDTO queryDTO) {
        Page<UserResponseDTO> page = userService.getUsersByPage(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "根据角色获取用户列表")
    @GetMapping("/role/{roleCode}")
    public Result<List<UserResponseDTO>> getUserByRole(@PathVariable String roleCode) {
        List<UserResponseDTO> users = userService.getUserByRole(roleCode);
        return Result.success(users);
    }

    @Operation(summary = "批量删除用户")
    @DeleteMapping("/deleteBatch")
    public Result<?> deleteBatch(@RequestBody UserBatchDeleteDTO batchDeleteDTO) {
        userService.deleteBatch(batchDeleteDTO);
        return Result.success();
    }

    @Operation(summary = "获取所有用户")
    @GetMapping("/all")
    public Result<List<UserResponseDTO>> getUserList() {
        List<UserResponseDTO> list = userService.getUserList();
        return Result.success(list);
    }

    @Operation(summary = "创建新用户")
    @PostMapping("/add")
    public Result<?> createUser(@RequestBody UserCreateDTO createDTO) {
        userService.createUser(createDTO);
        return Result.success();
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO updateDTO) {
        // 更新失败会抛出具体原因的异常
        userService.updateUser(id, updateDTO);
        return Result.success();
    }

    @Operation(summary = "根据id删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<?> deleteUserById(@PathVariable Long id) {
        // 删除失败会抛出异常
        userService.deleteUserById(id);
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/current")
    public Result<UserResponseDTO> getCurrentUser() {
        Long currentUserId = JwtTokenUtils.getCurrentUserId();
        if (currentUserId == null) {
            return Result.error("获取当前用户信息失败，请重新登录");
        }
        UserResponseDTO user = userService.getUserById(currentUserId);
        return Result.success(user);
    }

    @Operation(summary = "修改用户状态")
    @PutMapping("/status/{userId}")
    public Result<?> updateStatus(@PathVariable Long userId, @RequestBody UserStatusUpdateDTO statusUpdateDTO) {
        userService.updateUserStatus(userId, statusUpdateDTO);
        return Result.success();
    }

    @Operation(summary = "获取用户统计数据")
    @GetMapping("/stats")
    public Result<UserStatsDTO> getUserStats() {
        // 获取当前登录用户
        Long userId = JwtTokenUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        
        UserStatsDTO stats = userService.getUserStats(userId);
        return Result.success(stats);
    }

}
