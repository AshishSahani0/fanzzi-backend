package com.fanzzi.backend.post.service.delete;

import lombok.Data;

import java.util.List;

@Data
public class BulkDeletePostsRequest {

    private List<String> postIds;

}
