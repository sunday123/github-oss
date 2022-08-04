package com.ij34.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Description UploadDto
 * Date 2022/8/3
 * Created by www.ij34.com
 */
@Data
public class UploadDto {
    private String  owner;
    private String  repo;
    private String  path;
    private String  branch;
    private String  domain;
    private String  token;
    private Boolean isReName;

    private String fileStr;
    private String sha;
    private String url;

}
