package com.appsdeveloperblog.ws.urlshorten.auth.model.entity;

import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class Role extends BaseEntity {
    private String name;

    private String key;

    @ManyToMany
    @JoinTable(
            name = "role_authority",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id")
    )
    private Set<Authority> authorities = new HashSet<>();

    @ManyToMany(mappedBy = "roles")
    private Set<User> users = new HashSet<>();

}
