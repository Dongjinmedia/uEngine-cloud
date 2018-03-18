package org.uengine.cloud.group;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.gitlab4j.api.models.Member;
import org.hibernate.Hibernate;
import org.uengine.cloud.app.AppEntity;
import org.uengine.cloud.app.AppJpaRepository;
import org.uengine.iam.util.ApplicationContextRegistry;
import org.uengine.iam.util.JsonUtils;

import javax.persistence.*;
import java.util.*;

@Entity
@Table(name = "app_group")
public class AppGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String iam;

    @Column(name = "regDate", nullable = false, updatable = false, insertable = true)
    private long regDate;

    @PrePersist
    void preInsert() {
        this.regDate = new Date().getTime();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIam() {
        return iam;
    }

    public void setIam(String iam) {
        this.iam = iam;
    }

    public long getRegDate() {
        return regDate;
    }

    public void setRegDate(long regDate) {
        this.regDate = regDate;
    }
}
