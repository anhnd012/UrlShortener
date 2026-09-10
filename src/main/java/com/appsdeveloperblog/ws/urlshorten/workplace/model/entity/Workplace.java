package com.appsdeveloperblog.ws.urlshorten.workplace.model.entity;

import com.appsdeveloperblog.ws.urlshorten.auth.model.entity.User;
import com.appsdeveloperblog.ws.urlshorten.common.entity.BaseEntity;
import com.appsdeveloperblog.ws.urlshorten.url.entity.ShortUrl;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "workplace")
public class Workplace extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 500)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private User owner;

  @OneToMany(mappedBy = "workplace", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<WorkplaceMember> members = new ArrayList<>();

  @OneToMany(mappedBy = "workplace")
  private List<ShortUrl> urls = new ArrayList<>();
}
