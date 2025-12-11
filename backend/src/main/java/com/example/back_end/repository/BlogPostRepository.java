package com.example.back_end.repository;

import com.example.back_end.model.BlogPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    @EntityGraph(attributePaths = {"author"})
    Optional<BlogPost> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"author"})
    @Query("select p from BlogPost p where p.status = :status order by p.publishedAt desc, p.createdAt desc")
    List<BlogPost> findByStatusOrderByPublishedAtDesc(String status);

    @EntityGraph(attributePaths = {"author"})
    List<BlogPost> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    @EntityGraph(attributePaths = {"author"})
    @Query("select p from BlogPost p where p.status = 'pending' order by p.submittedAt asc, p.createdAt asc")
    List<BlogPost> findPending();

    @EntityGraph(attributePaths = {"author"})
    @Query("select p from BlogPost p where p.id = :id")
    Optional<BlogPost> findWithAuthorById(Long id);
}
