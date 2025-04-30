package com.devops.cicdtoolapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public  class JenkinsBuildApiResponse {
    private Integer number;
    private String url;
    private boolean building;
    private String result;
}