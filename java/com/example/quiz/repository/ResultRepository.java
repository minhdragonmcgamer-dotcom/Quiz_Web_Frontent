package com.example.quiz.repository;

import com.example.quiz.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {
    List<Result> findByUser_UserId(Long user_id);
    void deleteByQuiz_QuizId(Long quizId);
}