package dev.sivalabs.quicknotes.domain.repo;

import dev.sivalabs.quicknotes.domain.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends JpaRepository<Note, Long> {
    @Query("""
        select n from Note n
        where n.user.id = ?1 and n.archived = :isArchived
        """)
    Page<Note> findUserNotes(@Param("userId") Long userId, @Param("isArchived") boolean isArchived, Pageable pageable);

    @Query("""
            SELECT n FROM Note n
            WHERE n.user.id = :userId
            AND n.archived = false
            AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY n.createdAt DESC
            """)
    Page<Note> searchNonArchivedNotes(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Query("""
            SELECT n FROM Note n
            WHERE n.user.id = :userId
            AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY n.createdAt DESC
            """)
    Page<Note> searchAllNotes(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);
}
