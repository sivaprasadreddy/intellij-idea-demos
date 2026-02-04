package dev.sivalabs.quicknotes.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import dev.sivalabs.quicknotes.TestcontainersConfig;
import dev.sivalabs.quicknotes.domain.entity.Note;
import dev.sivalabs.quicknotes.domain.exception.ResourceNotFoundException;
import dev.sivalabs.quicknotes.domain.model.CreateNoteCmd;
import dev.sivalabs.quicknotes.domain.model.PagedResult;
import dev.sivalabs.quicknotes.domain.model.UpdateNoteCmd;
import dev.sivalabs.quicknotes.domain.repo.NoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest(webEnvironment = NONE)
@Import(TestcontainersConfig.class)
@Sql("/test-data.sql")
class NoteServiceTests {

    @Autowired
    private NoteService noteService;

    @Autowired
    private NoteRepository noteRepository;

    private static final Long ADMIN_USER_ID = 1L;
    private static final Long REGULAR_USER_ID = 2L;
    private static final Long NON_EXISTENT_USER_ID = 999L;

    @Test
    void shouldFindUserNotes() {
        PagedResult<Note> result = noteService.findUserNotes(ADMIN_USER_ID, 1);

        assertThat(result.data()).hasSize(10);
        assertThat(result.totalElements()).isEqualTo(10);
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
        // Verify all notes are non-archived
        assertThat(result.data()).allMatch(note -> !note.getArchived());
    }

    @Test
    void shouldFindUserNotesWithPagination() {
        // Demo user has 21 non-archived notes, PAGE_SIZE is 10
        PagedResult<Note> page1 = noteService.findUserNotes(REGULAR_USER_ID, 1);
        assertThat(page1.data()).hasSize(10);
        assertThat(page1.totalElements()).isEqualTo(21);
        assertThat(page1.hasNext()).isTrue();
        assertThat(page1.hasPrevious()).isFalse();

        PagedResult<Note> page2 = noteService.findUserNotes(REGULAR_USER_ID, 2);
        assertThat(page2.data()).hasSize(10);
        assertThat(page2.hasNext()).isTrue();
        assertThat(page2.hasPrevious()).isTrue();

        PagedResult<Note> page3 = noteService.findUserNotes(REGULAR_USER_ID, 3);
        assertThat(page3.data()).hasSize(1);
        assertThat(page3.hasNext()).isFalse();
        assertThat(page3.hasPrevious()).isTrue();
    }

    @Test
    void shouldReturnEmptyResultForUserWithNoNotes() {
        PagedResult<Note> result = noteService.findUserNotes(NON_EXISTENT_USER_ID, 1);

        assertThat(result.data()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void shouldFindUserArchivedNotes() {
        PagedResult<Note> result = noteService.findUserArchivedNotes(ADMIN_USER_ID, 1);

        assertThat(result.data()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        // Verify all notes are archived
        assertThat(result.data()).allMatch(Note::getArchived);
    }

    @Test
    void shouldSearchNotesExcludingArchived() {
        // Search for "Spring" - should find "Learning Goals 2025" (non-archived)
        PagedResult<Note> result = noteService.searchNotes(REGULAR_USER_ID, "Spring", 1, false);

        assertThat(result.data()).isNotEmpty();
        assertThat(result.data()).allMatch(note -> !note.getArchived());
        assertThat(result.data()).anyMatch(note -> note.getContent().contains("Spring"));
    }

    @Test
    void shouldSearchNotesIncludingArchived() {
        // Search for "Atomic" - "Book Club Discussion" is archived
        PagedResult<Note> result = noteService.searchNotes(REGULAR_USER_ID, "Atomic", 1, true);

        assertThat(result.data()).isNotEmpty();
        assertThat(result.data()).anyMatch(note -> note.getTitle().contains("Book Club"));
    }

    @Test
    void shouldReturnEmptyResultForNoSearchMatches() {
        PagedResult<Note> result = noteService.searchNotes(ADMIN_USER_ID, "xyz123nonexistent", 1, false);

        assertThat(result.data()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void shouldCreateNote() {
        CreateNoteCmd cmd = new CreateNoteCmd(ADMIN_USER_ID, "New Test Note", "Test content for new note");

        noteService.createNote(cmd);

        // Verify note was created - should now have 11 non-archived notes for admin
        PagedResult<Note> result = noteService.findUserNotes(ADMIN_USER_ID, 1);
        assertThat(result.totalElements()).isEqualTo(11);
        assertThat(result.data())
                .anyMatch(note -> note.getTitle().equals("New Test Note")
                        && note.getContent().equals("Test content for new note"));
    }

    @Test
    void shouldGetNoteById() {
        Note note = noteService.getNoteById(1L, ADMIN_USER_ID);

        assertThat(note).isNotNull();
        assertThat(note.getId()).isEqualTo(1L);
        assertThat(note.getTitle()).isEqualTo("Welcome to QuickNotes");
    }

    @Test
    void shouldThrowExceptionWhenNoteNotFound() {
        assertThatThrownBy(() -> noteService.getNoteById(999L, ADMIN_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    @Test
    void shouldThrowExceptionWhenAccessingOtherUserNote() {
        // Note 1 belongs to admin user, demo user should not access it
        assertThatThrownBy(() -> noteService.getNoteById(1L, REGULAR_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Note not found");
    }

    @Test
    void shouldUpdateNote() {
        UpdateNoteCmd cmd = new UpdateNoteCmd(1L, ADMIN_USER_ID, "Updated Title", "Updated content");

        noteService.updateNote(cmd);

        Note updated = noteService.getNoteById(1L, ADMIN_USER_ID);
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getContent()).isEqualTo("Updated content");
    }

    @Test
    void shouldThrowExceptionWhenUpdatingOtherUserNote() {
        UpdateNoteCmd cmd = new UpdateNoteCmd(1L, REGULAR_USER_ID, "Hacked Title", "Hacked content");

        assertThatThrownBy(() -> noteService.updateNote(cmd)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldDeleteNote() {
        noteService.deleteNote(1L, ADMIN_USER_ID);

        assertThatThrownBy(() -> noteService.getNoteById(1L, ADMIN_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenDeletingOtherUserNote() {
        assertThatThrownBy(() -> noteService.deleteNote(1L, REGULAR_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldArchiveNote() {
        // Note 1 is not archived initially
        Note note = noteService.getNoteById(1L, ADMIN_USER_ID);
        assertThat(note.getArchived()).isFalse();

        noteService.archiveNote(1L, ADMIN_USER_ID);

        Note archived = noteRepository.findById(1L).orElseThrow();
        assertThat(archived.getArchived()).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenArchivingOtherUserNote() {
        assertThatThrownBy(() -> noteService.archiveNote(1L, REGULAR_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldUnarchiveNote() {
        // Note 11 is archived (admin user)
        Note archivedNote = noteRepository.findById(11L).orElseThrow();
        assertThat(archivedNote.getArchived()).isTrue();

        noteService.unarchiveNote(11L, ADMIN_USER_ID);

        Note unarchived = noteRepository.findById(11L).orElseThrow();
        assertThat(unarchived.getArchived()).isFalse();
    }

    @Test
    void shouldThrowExceptionWhenUnarchivingOtherUserNote() {
        assertThatThrownBy(() -> noteService.unarchiveNote(11L, REGULAR_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
