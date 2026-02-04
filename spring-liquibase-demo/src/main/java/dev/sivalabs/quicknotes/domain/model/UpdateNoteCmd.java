package dev.sivalabs.quicknotes.domain.model;

public record UpdateNoteCmd(Long id, Long userId, String title, String content) {}
