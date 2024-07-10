package com.springboot.springsecurity.repository;

import com.springboot.springsecurity.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlogRepository extends JpaRepository<Article, Long> {
}

