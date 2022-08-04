package com.ij34.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Description GitUser
 * Date 2022/8/3
 * Created by www.ij34.com
 */
@Data
@Builder
@AllArgsConstructor
public class GitUser  implements Serializable {
    private String  owner;
    private String  repo;
    private String  path;
    private String  branch;
    private String  domain;
    private String  token;
    private Boolean isReName;
}
