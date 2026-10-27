package com.appsdeveloperblog.ws.urlshorten.auth.model.entity;

import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Authority extends BaseEntity {
    private String name;

    private String key;

    @ManyToMany(mappedBy = "authorities")
    private Set<Role> roles = new HashSet<>();
}
