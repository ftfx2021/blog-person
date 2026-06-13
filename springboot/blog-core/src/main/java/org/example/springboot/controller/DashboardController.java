package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.common.Result;
import org.example.springboot.dto.DashboardStatsResponseDTO;
import org.example.springboot.dto.DashboardTrendQueryDTO;
import org.example.springboot.dto.DashboardTrendResponseDTO;
import org.example.springboot.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "仪表盘接口")
@RestController
@RequestMapping("/dashboard")
@Slf4j
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public Result<DashboardStatsResponseDTO> getStats() {
        log.info("获取仪表盘统计数据");
        DashboardStatsResponseDTO stats = dashboardService.getStats();
        return Result.success(stats);
    }
    
    @Operation(summary = "获取文章发布趋势数据")
    @GetMapping("/article/trend")
    public Result<List<DashboardTrendResponseDTO>> getArticleTrend(@Valid @ModelAttribute DashboardTrendQueryDTO queryDTO) {
        log.info("获取文章发布趋势数据, days={}", queryDTO.getDays());
        List<DashboardTrendResponseDTO> trend = dashboardService.getArticleTrend(queryDTO.getDays());
        return Result.success(trend);
    }
    
    @Operation(summary = "获取访问量趋势数据")
    @GetMapping("/view/trend")
    public Result<List<DashboardTrendResponseDTO>> getViewTrend(@Valid @ModelAttribute DashboardTrendQueryDTO queryDTO) {
        log.info("获取访问量趋势数据, days={}", queryDTO.getDays());
        List<DashboardTrendResponseDTO> trend = dashboardService.getViewTrend(queryDTO.getDays());
        return Result.success(trend);
    }
} 