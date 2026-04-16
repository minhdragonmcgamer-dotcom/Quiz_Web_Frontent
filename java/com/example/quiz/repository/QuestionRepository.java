package com.example.quiz.repository;

import com.example.quiz.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    void deleteByQuestionId(Long questionId);
}