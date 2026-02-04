package dev.sivalabs.quicknotes.domain.model;

public record CreateUserCmd(String name, String email, String password, Role role) {}
