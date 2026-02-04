package dev.sivalabs.quicknotes.domain.service;

import dev.sivalabs.quicknotes.domain.entity.Note;
import dev.sivalabs.quicknotes.domain.exception.ResourceNotFoundException;
import dev.sivalabs.quicknotes.domain.model.CreateNoteCmd;
import dev.sivalabs.quicknotes.domain.model.PagedResult;
import dev.sivalabs.quicknotes.domain.model.UpdateNoteCmd;
import dev.sivalabs.quicknotes.domain.repo.NoteRepository;
import dev.sivalabs.quicknotes.domain.repo.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NoteService {
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    public PagedResult<Note> findUserNotes(Long userId, int pageNumber) {
        return findUserArchivedNotes(userId, false, pageNumber);
    }

    public PagedResult<Note> findUserArchivedNotes(Long userId, int pageNumber) {
        return findUserArchivedNotes(userId, true, pageNumber);
    }

    private PagedResult<Note> findUserArchivedNotes(Long userId, boolean isArchived, int pageNumber) {
        PageRequest pageRequest = getPageRequest(pageNumber);
        Page<Note> page = noteRepository.findUserNotes(userId, isArchived, pageRequest);
        return new PagedResult<>(page);
    }

    public PagedResult<Note> searchNotes(Long userId, String query, int pageNumber, boolean includeArchived) {
        PageRequest pageRequest = getPageRequest(pageNumber);
        Page<Note> page;
        if (includeArchived) {
            page = noteRepository.searchAllNotes(userId, query, pageRequest);
        } else {
            page = noteRepository.searchNonArchivedNotes(userId, query, pageRequest);
        }
        return new PagedResult<>(page);
    }

    private PageRequest getPageRequest(int pageNumber) {
        // Convert 1-indexed page to 0-indexed for Spring Data
        if (pageNumber <= 0) {
            pageNumber = 1;
        }
        return PageRequest.of(pageNumber - 1, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional
    public Note createNote(CreateNoteCmd cmd) {
        var user = userRepository.getReferenceById(cmd.userId());

        var note = new Note();
        note.setTitle(cmd.title());
        note.setContent(cmd.content());
        note.setUser(user);
        note.setArchived(false);

        return noteRepository.save(note);
    }

    public Note getNoteById(Long noteId, Long userId) {
        Note note = noteRepository
                .findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));

        // Security: Verify note belongs to the current user
        if (!note.getUser().getId().equals(userId)) {
            // throw new RuntimeException("Access denied: Note does not belong to user");
            // throwing NotFound to not reveal existence of a note owned by another user
            throw new ResourceNotFoundException("Note not found");
        }

        return note;
    }

    @Transactional
    public void updateNote(UpdateNoteCmd cmd) {
        Note note = getNoteById(cmd.id(), cmd.userId());
        note.setTitle(cmd.title());
        note.setContent(cmd.content());

        noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        Note note = getNoteById(noteId, userId);
        noteRepository.delete(note);
    }

    @Transactional
    public void archiveNote(Long noteId, Long userId) {
        Note note = getNoteById(noteId, userId);
        note.setArchived(true);
        noteRepository.save(note);
    }

    @Transactional
    public void unarchiveNote(Long noteId, Long userId) {
        Note note = getNoteById(noteId, userId);
        note.setArchived(false);
        noteRepository.save(note);
    }
}
