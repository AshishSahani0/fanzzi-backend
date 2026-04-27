package com.fanzzi.backend.channel.report.member.controller;

import com.fanzzi.backend.channel.report.dto.request.ReportRequest;
import com.fanzzi.backend.channel.report.service.ChannelReportService;
import com.fanzzi.backend.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/channels/{channelId}/reports")
@RequiredArgsConstructor
public class ChannelReportMemberController {

    private final ChannelReportService reportService;

    // =====================================================
    // 🚩 MEMBER REPORT CHANNEL
    // =====================================================
    /*
     * Endpoint used by channel members to report a channel.
     *
     * Flow:
     * Client → Submit report
     * ↓
     * Controller → Validate request
     * ↓
     * Service → Save report
     * ↓
     * Moderation pipeline triggered
     *
     * URL:
     * POST /api/channels/{channelId}/reports
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportSuccessResponse report(
            @PathVariable String channelId,
            @Valid @RequestBody ReportRequest request
    ) {

        // Extract authenticated user from security context
        String userId = SecurityUtil.getCurrentUserId();

        // Delegate report processing to service layer
        reportService.reportChannel(
                channelId,
                userId,
                request.getReason(),           // Enum reason
                request.getDescription(),      // Optional description
                request.getEvidenceMediaKey()  // Optional media evidence key
        );

        return new ReportSuccessResponse(
                "Report submitted successfully"
        );
    }

    // =====================================================
    // RESPONSE DTO
    // =====================================================
    /*
     * Lightweight success response returned after
     * report submission.
     */
    public record ReportSuccessResponse(String message) {}
}

