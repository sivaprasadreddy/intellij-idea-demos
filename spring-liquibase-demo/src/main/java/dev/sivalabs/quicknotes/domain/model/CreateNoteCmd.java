package dev.sivalabs.quicknotes.domain.model;

public record CreateNoteCmd(Long userId, String title, String content) {}
